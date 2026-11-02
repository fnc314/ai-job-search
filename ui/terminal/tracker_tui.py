import curses
import datetime
import json
import logging
import os
import queue
import re
import sys
import textwrap
import threading
import time
import traceback
from pathlib import Path

try:
    from textual.app import App
    from textual.screen import Screen
    from textual.widgets import Label
    from textual.binding import Binding
    TEXTUAL_AVAILABLE = True
except ImportError:
    TEXTUAL_AVAILABLE = False

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
RECALC_QUEUE_PATH = REPO_ROOT / ".recalc_queue"

# ─── Logging Infrastructure ───────────────────────────────────────────────────
# Persistent logs live in .logs/tui/YYYYMMDD/ at the repo root. Each invocation
# gets a timestamped log file within a date-based subdirectory. Crashes and
# errors are always captured to a separate crash log so they survive even when
# the TUI exits abruptly.

LOGS_BASE_DIR = REPO_ROOT / ".logs" / "tui"
_tui_logger: logging.Logger | None = None
crash_logger: logging.Logger | None = None
_log_file_path: Path | None = None
crash_log_file_path: Path | None = None
_date_log_dir: Path | None = None


def _setup_logging() -> None:
    """Initialize file-based logging for this TUI invocation.

    Creates .logs/tui/YYYYMMDD/ if needed and opens two loggers:
    - _tui_logger: general operational log (TUI_HHMMSS.log)
    - crash_logger: crash & error log (CRASH_HHMMSS.log)

    Both loggers write to separate files so crash logs survive even when
    the general log is truncated or rotated. Logs are organized by date
    to enable easy cleanup of old logs.
    """
    global _tui_logger, crash_logger, _log_file_path, crash_log_file_path, _date_log_dir

    now = datetime.datetime.now()
    date_str = now.strftime("%Y%m%d")
    _date_log_dir = LOGS_BASE_DIR / date_str
    _date_log_dir.mkdir(parents=True, exist_ok=True)
    timestamp = now.strftime("%H%M%S")

    # General invocation log
    _log_file_path = _date_log_dir / f"TUI_{timestamp}.log"
    _tui_logger = logging.getLogger("tui")
    _tui_logger.setLevel(logging.DEBUG)
    _tui_logger.handlers.clear()
    _fh = logging.FileHandler(_log_file_path, mode="a", encoding="utf-8")
    _fh.setFormatter(logging.Formatter(
        "%(asctime)s [%(levelname)s] %(message)s", datefmt="%H:%M:%S"
    ))
    _tui_logger.addHandler(_fh)

    # Crash log (separate file so it survives abrupt exits)
    crash_log_file_path = _date_log_dir / f"CRASH_{timestamp}.log"
    crash_logger = logging.getLogger("tui.crash")
    crash_logger.setLevel(logging.ERROR)
    crash_logger.handlers.clear()
    _cfh = logging.FileHandler(crash_log_file_path, mode="a", encoding="utf-8")
    _cfh.setFormatter(logging.Formatter(
        "%(asctime)s [%(levelname)s] %(message)s", datefmt="%H:%M:%S"
    ))
    crash_logger.addHandler(_cfh)

    _tui_logger.info("TUI invocation started")
    _tui_logger.info("Log directory: %s", _date_log_dir)
    _tui_logger.info("Log file: %s", _log_file_path)
    _tui_logger.info("Crash log: %s", crash_log_file_path)


def log(msg: str, *args, **kwargs) -> None:
    """Convenience wrapper for the TUI logger."""
    if _tui_logger:
        _tui_logger.debug(msg, *args, **kwargs)


def log_error(msg: str, *args, **kwargs) -> None:
    """Log an error to both the general and crash loggers."""
    if _tui_logger:
        _tui_logger.error(msg, *args, **kwargs)
    if crash_logger:
        crash_logger.error(msg, *args, **kwargs)


def log_crash(exc: BaseException) -> None:
    """Record a full crash traceback to the crash log."""
    if crash_logger:
        tb = traceback.format_exception(type(exc), exc, exc.__traceback__)
        crash_logger.error("\n".join(tb))

DEFAULT_CONFIG = {
    "columns": [
        {"key": "date", "label": "Date", "width": 12, "visible": True},
        {"key": "company", "label": "Company", "width": 20, "visible": True},
        {"key": "status", "label": "Status", "width": 12, "visible": True},
        {"key": "fit_rating", "label": "Fit", "width": 5, "visible": True},
        {"key": "role", "label": "Role", "width": 0, "visible": True}
    ],
    "theme": "dark",
    "split_mode": True
}

def get_config_path():
    xdg_config = os.environ.get("XDG_CONFIG_HOME")
    if xdg_config:
        base = Path(xdg_config)
    else:
        base = Path.home() / ".config"
    
    app_config_dir = base / "job-tracker"
    config_file = app_config_dir / "config.json"
    
    try:
        app_config_dir.mkdir(parents=True, exist_ok=True)
        return config_file
    except Exception:
        local_cfg = Path(__file__).parent / "tui_config.json"
        if local_cfg.exists():
            return local_cfg
        return Path(__file__).parent / "config.json"

def load_config():
    path = get_config_path()
    if path.exists():
        try:
            with open(path, 'r') as f:
                config = json.load(f)
                # Ensure all default keys exist
                for key, val in DEFAULT_CONFIG.items():
                    if key not in config:
                        config[key] = val
                return config
        except Exception:
            pass

    # Fallback to local tui_config.json
    local_cfg = Path(__file__).parent / "tui_config.json"
    if local_cfg.exists():
        try:
            with open(local_cfg, 'r') as f:
                config = json.load(f)
                for key, val in DEFAULT_CONFIG.items():
                    if key not in config:
                        config[key] = val
                return config
        except Exception:
            pass

    return DEFAULT_CONFIG.copy()

def save_config(config):
    path = get_config_path()
    try:
        path.parent.mkdir(parents=True, exist_ok=True)
        with open(path, 'w') as f:
            json.dump(config, f, indent=2)
    except Exception:
        pass

def _load_reader():
    """Import the shared store reader without making the scripts tree a package."""
    scripts = REPO_ROOT / "scripts"
    if str(scripts) not in sys.path:
        sys.path.insert(0, str(scripts))
    import reader  # noqa: PLC0415 - path is configured just above
    return reader

DATA_META = {"source": "unknown"}

# Reactive refresh state — shared between the background poller and the main loop
_refresh_lock = threading.Lock()
_refresh_rows = None
_refresh_error = None
_refresh_requested = threading.Event()
_refresh_thread = None
_REFRESH_INTERVAL = 60  # seconds between background Firestore polls

# Loading state — pushed by the RxPY stream from reader.py.
# The animation thread reads this; the data thread never writes it directly.
_loading_lock = threading.Lock()
_loading_state: int = 1  # LoadingState.INITIALIZING
# Queue of state changes — each state emitted by reader.py is enqueued so the
# animation thread drains them one by one. This guarantees every state is
# rendered exactly once, even when multiple states fire within microseconds.
_loading_queue = queue.Queue()

# Callback registered with reader.py to receive loading state changes.
# Set once at startup, no cleanup needed.
_state_registered = False


# ─── Textual Loading Screen ───────────────────────────────────────────────────
# A lightweight Textual app that shows animated dots and real-time status text
# while Firestore data loads. Uses call_from_thread for reliable state delivery
# from the background data thread. Once loading completes, the app exits and
# returns the loaded rows so the curses table can take over.

if TEXTUAL_AVAILABLE:

    class LoadingScreen(Screen):
        """Animated loading screen with two dot rows and status text."""

        # `q` always quits — no confirmation toast, no stacking
        BINDINGS = [
            Binding("q", "quit", "Quit", show=True),
        ]

        # Top dots: 0→1→2→3→3→2→1→0→repeat (period 8)
        DOTS_CYCLE = ["", "◉", "◉ ◉", "◉ ◉ ◉", "◉ ◉ ◉", "◉ ◉", "◉", ""]
        HALF_PERIOD = len(DOTS_CYCLE) // 2  # 4

        def __init__(self, status_callback):
            super().__init__()
            self._status_callback = status_callback
            self._frame = 0
            self._state_label = "Initializing"
            self._timer_token: object | None = None
            # Queue for incoming states — each state is rendered individually
            # with a small delay so rapid successive states are all visible
            self._state_queue: "queue.Queue[int]" = queue.Queue()
            # Cancellation flag: set when user quits so the background thread
            # stops trying to access self.app after the app has shut down.
            self._cancelled = False

        def compose(self):
            # Three centered labels: top dots, status, bottom dots
            yield Label("", id="top_dots")
            yield Label("", id="status")
            yield Label("", id="bot_dots")

        def on_mount(self) -> None:
            # Set initial status text immediately so the label is not blank
            # before the first state arrives from reader.py
            self._state_label = "Initializing"
            try:
                status_label = self.query_one("#status", Label)
                status_label.update(f" {self._state_label} ")
            except Exception:
                pass
            # Start animation timer (200ms per frame = 5fps)
            self._timer_token = self.set_interval(0.2, self._tick)
            # Register the state callback with reader.py
            self._register_callback()
            # Start data loading in background
            self._start_data_thread()

        def _register_callback(self) -> None:
            """Register _on_state_change so reader.py delivers states to this screen."""
            try:
                reader = _load_reader()
                reader._on_state_change = self._on_state_change
            except Exception:
                pass

        def _on_state_change(self, state) -> None:
            """Called from reader.py's background thread — enqueue the state.

            States are enqueued so rapid successive states (e.g. PROCESSING →
            CHECKING_PROPOSALS) are rendered one at a time with a visible delay,
            instead of being coalesced by call_from_thread.

            If the screen was cancelled (user pressed q/Ctrl+C), silently ignore
            the state — the app may already be shutting down and self.app access
            would raise NoActiveAppError.
            """
            if self._cancelled:
                return
            self._state_queue.put(int(state))
            try:
                self.app.call_from_thread(self._drain_state_queue)
            except Exception:
                # App is shutting down; NoActiveAppError or similar — ignore
                pass

        def _drain_state_queue(self) -> None:
            """Drain one state from the queue and render it.

            If more states are pending, schedules the next drain after a short
            delay so each state is visible to the user.
            """
            try:
                state_val = self._state_queue.get_nowait()
            except queue.Empty:
                return

            try:
                reader = _load_reader()
                label = reader.LoadingState(state_val).label()
            except (ValueError, ImportError, AttributeError):
                label = "Loading..."

            self._state_label = label
            try:
                status_label = self.query_one("#status", Label)
                status_label.update(f" {label} ")
                status_label.refresh()
            except Exception:
                pass

            # If more states are queued, schedule the next drain
            if not self._state_queue.empty():
                self.set_timer(0.25, self._drain_state_queue)
            # If complete, stop the animation timer
            elif state_val >= 6:
                if self._timer_token is not None:
                    self._timer_token.stop()
                    self._timer_token = None

        def _start_data_thread(self) -> None:
            """Start Firestore fetch in a background thread."""
            def _load():
                try:
                    rows, error = load_data()
                    # If the user quit while loading, self.app is gone — guard it
                    if self._cancelled:
                        return
                    try:
                        self.app.call_from_thread(self._on_load_complete, rows, error)
                    except Exception:
                        # NoActiveAppError: app already shut down — ignore
                        pass
                except Exception as e:
                    if self._cancelled:
                        return
                    try:
                        self.app.call_from_thread(self._on_load_complete, [], str(e))
                    except Exception:
                        # NoActiveAppError: app already shut down — ignore
                        pass

            t = threading.Thread(target=_load, daemon=True)
            t.start()

        def _on_load_complete(self, rows, error) -> None:
            """Called on the main event loop thread when data loading finishes."""
            # Store result for retrieval after app exits
            self._load_result = (rows, error)
            # Exit the app to return to curses
            self.app.exit(result=(rows, error))

        def _tick(self) -> None:
            """Advance animation frame and redraw dots only.

            The status label is updated by _update_state() via call_from_thread
            and should NOT be overwritten here — _update_state() sets the correct
            label from reader.py, and _tick() only animates the dot rows.
            """
            self._frame = (self._frame + 1) % len(self.DOTS_CYCLE)
            top = self.DOTS_CYCLE[self._frame]
            bot = self.DOTS_CYCLE[(self._frame + self.HALF_PERIOD) % len(self.DOTS_CYCLE)]

            top_label = self.query_one("#top_dots", Label)
            bot_label = self.query_one("#bot_dots", Label)

            top_label.update(f" {top} ")
            bot_label.update(f" {bot} ")

        def action_quit(self) -> None:
            """Handle quit request — cancel the background thread and exit."""
            self._cancelled = True
            # Clean up the state callback so the background thread stops calling it
            try:
                reader = _load_reader()
                reader._on_state_change = None
            except Exception:
                pass
            # Stop the animation timer
            if self._timer_token is not None:
                self._timer_token.stop()
                self._timer_token = None
            # Exit the app — returns empty rows so curses can still start
            self.app.exit(result=([], "Cancelled by user"))

        def on_unmount(self) -> None:
            """Clean up the state callback."""
            self._cancelled = True
            try:
                reader = _load_reader()
                reader._on_state_change = None
            except Exception:
                pass

    class LoadingApp(App):
        """Minimal Textual app that runs the loading screen and exits on complete."""

        CSS = """
            Screen {
                align: center middle;
            }
            Label {
                content-align: center middle;
                width: 100%;
            }
            #top_dots {
                color: $accent;
                text-style: bold;
            }
            #status {
                color: $text;
            }
            #bot_dots {
                color: $accent;
                text-style: bold;
            }
        """

        # Override default Ctrl+C to exit cleanly without stacking toasts
        BINDINGS = [
            Binding("ctrl+c", "quit", "Quit", show=False),
        ]

        def on_mount(self) -> None:
            # Push the loading screen — status_callback is a no-op because
            # the screen registers its own callback via call_from_thread
            self.push_screen(LoadingScreen(lambda s: None))

        def action_quit(self) -> None:
            """Handle quit (Ctrl+C) — cancel the loading screen before exiting."""
            # Cancel the loading screen's background thread so it doesn't
            # try to access self.app after we shut down.
            try:
                screen = self.screen
                if isinstance(screen, LoadingScreen):
                    screen._cancelled = True
                    try:
                        reader = _load_reader()
                        reader._on_state_change = None
                    except Exception:
                        pass
            except Exception:
                pass
            self.exit(result=([], "Cancelled by user"))

    def run_textual_loading() -> tuple:
        """Run the Textual loading screen and return (rows, error) when complete.

        This blocks until loading finishes, then returns the loaded data
        so the curses table can take over. After Textual exits, the terminal
        is reset so curses can initialize cleanly.
        """
        # Save the original terminal attributes before Textual modifies them.
        # We need to restore these after Textual exits so curses can initialize.
        # tcgetattr fails with OSError in non-TTY environments (e.g. CI), which
        # is fine — curses won't work there either.
        import termios
        original_tty = None
        try:
            original_tty = termios.tcgetattr(sys.stdin.fileno())
        except Exception:  # noqa: BLE001 - termios.error in non-TTY env
            pass

        app = LoadingApp()
        result = app.run()

        # Restore the original terminal attributes so curses can take over.
        # Textual leaves the tty in raw mode; curses needs cooked mode to
        # call setraw() itself during initialization.
        if original_tty is not None:
            try:
                termios.tcsetattr(sys.stdin.fileno(), termios.TCSAFLUSH, original_tty)
            except Exception:  # noqa: BLE001 - termios.error
                pass
        sys.stdout.write("\033[2J\033[H")  # Clear screen and home cursor
        sys.stdout.flush()

        # result is (rows, error) from LoadingScreen._on_load_complete
        if result is not None:
            return result
        # Fallback: try to get result from the screen
        try:
            screen = app.screen
            if hasattr(screen, '_load_result'):
                return screen._load_result
        except Exception:
            pass
        # Last resort: load synchronously
        return load_data()

else:
    def run_textual_loading() -> tuple:
        """Fallback when Textual is not available — loads data synchronously."""
        return load_data()


def write_recalc_request(row):
    """Ask for a fit recalculation without performing one.

    The TUI cannot compute a score: that comes from the evaluation rubric. So it
    queues a structured request keyed on the application id, which `/apply` or
    an agent resolves with `tracker.py score`. Writing the whole legacy row (as
    the old queue did) meant the consumer had to re-derive company, role, and id
    from free text, and it could not tell which generation of the shape it got.
    """
    request = {
        "schemaVersion": 4,
        "kind": "fit-recalculation",
        "requestedAt": datetime.datetime.now().astimezone().isoformat(timespec="seconds"),
        "applicationId": row.get("id"),
        "company": row.get("company"),
        "role": row.get("role"),
        "currentFitRating": row.get("fit_rating") or None,
    }
    try:
        with open(RECALC_QUEUE_PATH, "w") as handle:
            json.dump(request, handle, indent=2)
    except OSError:
        pass
    return request

def _get_filters() -> dict:
    """Read filter flags passed from tracker.py --tui."""
    return {
        "open": os.environ.get("TRACKER_FILTER_OPEN", "") == "1",
        "status": os.environ.get("TRACKER_FILTER_STATUS", ""),
        "tag": os.environ.get("TRACKER_FILTER_TAG", ""),
        "limit": int(os.environ.get("TRACKER_FILTER_LIMIT", "0") or "0"),
    }


def _apply_filters(rows: list[dict], filters: dict) -> list[dict]:
    """Apply tracker.py --tui filters to the full row set."""
    filtered = rows

    # Filter to open applications only (status is not closed/withdrawn/declined)
    if filters.get("open"):
        closed_statuses = {"closed", "withdrawn", "declined", "rejected", "hired"}
        filtered = [
            row for row in filtered
            if row.get("status", "").lower() not in closed_statuses
        ]

    # Filter by specific status
    if filters.get("status"):
        target = filters["status"].lower()
        filtered = [
            row for row in filtered
            if row.get("status", "").lower() == target
        ]

    # Filter by tag (tags are stored in notes or as a separate field)
    if filters.get("tag"):
        tag = filters["tag"]
        filtered = [
            row for row in filtered
            if tag in (row.get("notes") or "")
            or tag in (row.get("sector") or "")
        ]

    # Apply limit
    if filters.get("limit", 0) > 0:
        filtered = filtered[:filters["limit"]]

    return filtered


def _filters_label(filters: dict) -> str:
    """Return a short label for active filters, e.g. 'open' or 'open + limit:50'."""
    parts = []
    if filters.get("open"):
        parts.append("open")
    if filters.get("status"):
        parts.append(f"status:{filters['status']}")
    if filters.get("tag"):
        parts.append(f"tag:{filters['tag']}")
    if filters.get("limit", 0) > 0:
        parts.append(f"limit:{filters['limit']}")
    return " + ".join(parts) if parts else ""


def _set_loading_state(state):
    """Queue a loading state change for the animation thread.

    Called by reader.py's _on_state_change callback. Accepts a LoadingState enum
    value (int). Each call enqueues the state so the animation thread drains them
    one by one — no states are lost, even when multiple fire within microseconds.
    """
    global _loading_state
    state_int = int(state)
    with _loading_lock:
        _loading_state = state_int
    _loading_queue.put(state_int)


def _register_state_callback():
    """Register _set_loading_state with reader.py so state changes are delivered.

    Called once at startup. reader.py calls the registered callback synchronously
    on the calling thread for each state change.
    """
    global _state_registered
    if _state_registered:
        return
    try:
        module = _load_reader()
        module._on_state_change = _set_loading_state
        _state_registered = True
    except Exception:
        pass


def _fetch_rows():
    """Core Firestore fetch + filter logic, shared by initial load and background refresh."""
    module = None
    try:
        module = _load_reader()
    except Exception as error:  # noqa: BLE001 - a read failure must not crash curses
        error_state = module.LoadingState.ERROR if module and hasattr(module, 'LoadingState') else 100
        _set_loading_state(error_state)
        return None, str(error), None

    try:
        rows, meta = module.rows()
    except Exception as error:  # noqa: BLE001 - a read failure must not crash curses
        error_state = module.LoadingState.ERROR if module and hasattr(module, 'LoadingState') else 100
        _set_loading_state(error_state)
        return None, str(error), None

    filters = _get_filters()
    rows = _apply_filters(rows, filters)

    label = module.badge(meta)
    filter_label = _filters_label(filters)
    if filter_label:
        label = f"{label} [{filter_label}]"

    meta = {**meta, "label": label}
    return rows, None, meta


def load_data():
    """Rows come from the tracker store, then the cache, then the CSV.

    The legacy row shape is preserved by `scripts/reader.py`, so every renderer
    below is unchanged. `DATA_META` records which source answered, and the
    header shows it: a stale snapshot must never look live.

    If launched via `tracker.py list --tui`, filter flags are read from
    environment variables and applied after loading.
    """
    global DATA_META
    log("load_data() called")
    rows, error, meta = _fetch_rows()
    if error is not None:
        DATA_META = {"source": "unavailable", "label": "unavailable", "error": error}
        log_error("load_data() failed: %s", error)
        return [], error
    DATA_META = meta
    log("load_data() returned %d rows from %s", len(rows) if rows else 0, meta.get("source", "unknown"))
    return rows, None


def _background_poller():
    """Periodically poll Firestore in the background and signal the main loop."""
    global _refresh_rows, _refresh_error
    while not _refresh_requested.is_set():
        if _refresh_requested.wait(timeout=_REFRESH_INTERVAL):
            log("Background poller shutting down")
            break  # Shutdown signal
        try:
            rows, error, meta = _fetch_rows()
            if error is not None:
                log_error("Background refresh error: %s", error)
            else:
                log("Background refresh: %d rows", len(rows) if rows else 0)
            with _refresh_lock:
                _refresh_rows = rows
                _refresh_error = error
                if meta is not None:
                    DATA_META.update(meta)
            _refresh_requested.set()  # Signal main loop that new data is available
        except Exception as e:
            log_error("Background poller exception: %s", e)


def start_background_refresh():
    """Start the background Firestore poller thread."""
    global _refresh_thread
    _refresh_requested.clear()
    _refresh_thread = threading.Thread(target=_background_poller, daemon=True)
    _refresh_thread.start()


def stop_background_refresh():
    """Signal the background poller to stop."""
    global _refresh_thread
    _refresh_requested.set()
    if _refresh_thread is not None:
        _refresh_thread.join(timeout=2)
        _refresh_thread = None


def check_background_refresh() -> tuple:
    """Check if the background poller has new data. Returns (rows, error) or (None, None)."""
    global _refresh_rows, _refresh_error
    with _refresh_lock:
        if _refresh_rows is not None:
            rows = _refresh_rows
            error = _refresh_error
            _refresh_rows = None
            _refresh_error = None
            return rows, error
    return None, None

def get_column_widths(rows, cols_config):
    widths = {}
    for col in cols_config:
        key = col["key"]
        label = col["label"]
        if key == "role":
            widths[key] = 0 # Flex
            continue
        # Minimum width to accommodate label + sort indicator [N] Label v
        max_w = len(label) + 6
        for row in rows:
            val = str(row.get(key, "") or "")
            if key == "fit_rating" and not val:
                val = "?? RECALC"
            max_w = max(max_w, len(val))
        widths[key] = max_w
    return widths

def get_archive_dir(row):
    base = "job-applications"
    if not os.path.exists(base):
        candidate = REPO_ROOT / "job-applications"
        if candidate.exists():
            base = str(candidate)
    if os.path.exists(base):
        c_raw = row.get('company', '').strip()
        c_norm = re.sub(r'[^a-zA-Z0-9]', '', c_raw).lower()
        for drop in ['inc', 'llc', 'ltd', 'as', 'corp', 'corporation', 'group']:
            if c_norm.endswith(drop):
                c_norm = c_norm[:-len(drop)]
                
        matching_comp = None
        for c in os.listdir(base):
            cn = re.sub(r'[^a-zA-Z0-9]', '', c).lower()
            if cn == c_norm or c_norm in cn or cn in c_norm:
                matching_comp = c
                break
        if matching_comp:
            comp_path = os.path.join(base, matching_comp)
            roles = [r for r in os.listdir(comp_path) if not r.startswith('.')]
            if roles:
                role_raw = row.get('role', '').strip()
                r_norm = re.sub(r'[^a-zA-Z0-9]', '', role_raw).lower() if role_raw else 'general'
                matching_role = None
                for r in roles:
                    rn = re.sub(r'[^a-zA-Z0-9]', '', r).lower()
                    if rn == r_norm or r_norm in rn or rn in r_norm:
                        matching_role = r
                        break
                if not matching_role:
                    matching_role = roles[0]
                role_path = os.path.join(comp_path, matching_role)
                dates = [d for d in os.listdir(role_path) if not d.startswith('.')]
                if dates:
                    date_raw = row.get('date', '').strip()
                    if date_raw in dates:
                        return os.path.join(role_path, date_raw)
                    return os.path.join(role_path, dates[0])
                return role_path
            return comp_path

    company = row.get('company', '').lower().strip()
    for drop in ['inc.', 'inc', 'llc', 'ltd', 'a/s', 'corp', 'corporation', 'group']:
        company = company.replace(f' {drop}', '')
    company = company.replace(' ', '_').replace('.', '').replace(',', '')
    role = row.get('role', '').lower().replace(' ', '_').replace('.', '').replace(',', '').replace('(', '').replace(')', '')
    return f"job-applications/{company}_{role}"

def load_detail_lines(row, max_width):
    raw_entries = [] # List of (text, file_path_or_url)
    
    # Application summary
    raw_entries.append((f"COMPANY: {row.get('company')}", None))
    raw_entries.append((f"ROLE:    {row.get('role')}", None))
    raw_entries.append((f"DATE:    {row.get('date')}", None))
    raw_entries.append((f"STATUS:  {row.get('status')} (Fit: {row.get('fit_rating')})", None))
    raw_entries.append((f"NOTES:   {row.get('notes')}", None))
    source_url = row.get('source')
    if source_url:
        raw_entries.append((f"URL:     {source_url}", source_url))
    raw_entries.append(("", None))
    
    # Company profile from Firestore (merged by reader.py as _company)
    company_data = row.get('_company')
    if company_data:
        raw_entries.append(("--- COMPANY PROFILE ---", None))
        for field, label in [
            ("name", "Name"), ("sector", "Sector"), ("website", "Website"),
            ("hqAddress", "HQ"), ("careersEmail", "Careers Email"),
            ("jobBoardUrl", "Job Board"), ("linkedinUrl", "LinkedIn"),
            ("careersUrl", "Careers URL"),
        ]:
            value = company_data.get(field)
            if value:
                raw_entries.append((f"  {label}: {value}", None))
        raw_entries.append(("", None))
    
    # Files section — GCS storage from Firestore, local files as fallback
    raw_entries.append(("FILES (Enter to Open):", None))
    artifacts = row.get('artifacts', [])
    has_files = False
    for artifact in artifacts:
        filename = artifact.get('filename', 'unknown')
        kind = artifact.get('kind', '').split(':', 1)[-1] if artifact.get('kind') else 'file'
        storage = artifact.get('storage')
        local_path = artifact.get('localPath')
        external_url = artifact.get('externalUrl')
        
        if storage and storage.get('path'):
            # Show GCS storage path; generate signed URL on demand
            raw_entries.append((f"  [GCS] {kind}: {filename}", storage.get('path', '')))
            has_files = True
        elif external_url:
            raw_entries.append((f"  [LINK] {kind}: {filename}", external_url))
            has_files = True
        elif local_path and os.path.exists(local_path):
            raw_entries.append((f"  [FILE] {kind}: {filename}", local_path))
            has_files = True
    
    # Legacy fallback: scan local archive dir if no artifacts from Firestore
    if not has_files:
        archive_dir = get_archive_dir(row)
        candidate_files = []
        if os.path.exists(archive_dir):
            for f in os.listdir(archive_dir):
                if os.path.isfile(os.path.join(archive_dir, f)):
                    candidate_files.append(os.path.join(archive_dir, f))
        cv_file = row.get('cv_file')
        if cv_file and os.path.exists(cv_file):
            candidate_files.append(cv_file)
        cl_file = row.get('cover_letter_file')
        if cl_file and os.path.exists(cl_file):
            candidate_files.append(cl_file)
        seen = set()
        for fpath in candidate_files:
            if fpath not in seen:
                raw_entries.append((f"  [FILE] {os.path.basename(fpath)}", fpath))
                seen.add(fpath)
    
    raw_entries.append(("", None))

    # Outreach section (static config, kept from local JSON)
    try:
        import urllib.parse
        proto_path = ".claude/skills/job-application-assistant/outreach_protocols.json"
        if not os.path.exists(proto_path):
            candidate = REPO_ROOT / ".claude" / "skills" / "job-application-assistant" / "outreach_protocols.json"
            if candidate.exists():
                proto_path = str(candidate)
        with open(proto_path, "r") as f:
            protocols = json.load(f)

        comp = row.get('company', 'Company')
        role = row.get('role', 'Role')
        r_url = protocols.get("search_urls", {}).get("recruiter", "").replace("{company}", urllib.parse.quote(comp))
        p_url = protocols.get("search_urls", {}).get("peer", "").replace("{company}", urllib.parse.quote(comp)).replace("{role_keyword}", urllib.parse.quote(role))

        raw_entries.append(("--- OUTREACH PROTOCOLS ---", None))
        raw_entries.append((f"  [LINK] Recruiter Search", r_url))
        raw_entries.append((f"  [LINK] Peer Search", p_url))
        raw_entries.append(("", None))

        raw_entries.append(("  RECRUITER MESSAGE TEMPLATE:", None))
        r_tpl = protocols.get("templates", {}).get("recruiter", "").replace("{role}", role)
        for line in r_tpl.splitlines():
            raw_entries.append((f"  {line}", None))

        raw_entries.append(("", None))

        raw_entries.append(("  PEER MESSAGE TEMPLATE:", None))
        p_tpl = protocols.get("templates", {}).get("peer", "").replace("{role}", role)
        for line in p_tpl.splitlines():
            raw_entries.append((f"  {line}", None))

        raw_entries.append(("", None))
    except Exception as e:
        raw_entries.append((f"--- Could not load outreach protocols: {e} ---", None))
        raw_entries.append(("", None))

    wrapped_lines = []
    for text, fpath in raw_entries:
        text = text.replace('\t', '    ')
        if text.strip() == "":
            wrapped_lines.append(("", fpath))
        else:
            wrapped = textwrap.wrap(text, width=max_width - 2)
            for w_line in wrapped:
                wrapped_lines.append((w_line, fpath))
            
    return wrapped_lines

def safe_addstr(stdscr, y, x, text, attr=0):
    """Safely add a string to the screen, ignoring bottom-right corner errors."""
    try:
        stdscr.addstr(y, x, text, attr)
    except curses.error:
        pass

def init_colors(is_light_theme, no_color):
    if no_color:
        curses.init_pair(1, curses.COLOR_BLACK, curses.COLOR_WHITE) 
        curses.init_pair(2, curses.COLOR_BLACK, curses.COLOR_WHITE) 
        curses.init_pair(3, curses.COLOR_WHITE, curses.COLOR_BLACK) 
        curses.init_pair(4, curses.COLOR_WHITE, curses.COLOR_BLACK) 
    else:
        if is_light_theme:
            curses.init_pair(1, curses.COLOR_WHITE, curses.COLOR_BLUE)  
            curses.init_pair(2, curses.COLOR_WHITE, curses.COLOR_BLACK) 
            curses.init_pair(3, curses.COLOR_BLUE, curses.COLOR_WHITE)  
            curses.init_pair(4, curses.COLOR_BLACK, curses.COLOR_WHITE) 
        else:
            curses.init_pair(1, curses.COLOR_BLACK, curses.COLOR_CYAN)  
            curses.init_pair(2, curses.COLOR_BLACK, curses.COLOR_WHITE) 
            curses.init_pair(3, curses.COLOR_CYAN, curses.COLOR_BLACK)  
            curses.init_pair(4, curses.COLOR_WHITE, curses.COLOR_BLACK) 

def _get_loading_state() -> int:
    """Read the current loading state as an int value (thread-safe)."""
    with _loading_lock:
        return _loading_state


def _loading_state_label(state_val: int) -> str:
    """Convert a LoadingState int value to a display label.

    Lazily imports reader.LoadingState so this works even if the import
    fails (falls back to a generic message).
    """
    try:
        import reader  # noqa: PLC0415
        ls = reader.LoadingState(state_val)
        return ls.label()
    except (ValueError, ImportError, AttributeError):
        return "Loading..."


def _show_loading(stdscr, message, is_light_theme, no_color, data_thread: threading.Thread, animation_ready: threading.Event):
    """Show an animated loading message while data loads in the background.

    Two rows of animated dots (above and below) frame a single status text line.
    Top dots follow 0→1→2→3→3→2→1→0, bottom dots animate half-period offset
    (3→2→1→0→0→1→2→3). Uses time.sleep() instead of nodelay/getch to avoid
    leaving the curses buffer in a stale state on Ghostty.

    Signals animation_ready after the first frame so the data thread knows
    it can start updating _loading_state.
    """
    # Colors are already initialized by main() before this is called.
    stdscr.bkgd(' ', curses.color_pair(4) if curses.has_colors() else 0)
    curses.curs_set(0)

    h, w = stdscr.getmaxyx()
    if h < 3 or w < 10:
        return False

    c_highlight = curses.color_pair(3) | curses.A_BOLD if curses.has_colors() else curses.A_BOLD
    c_default = curses.color_pair(4) if curses.has_colors() else 0

    # Top dots: 0→1→2→3→3→2→1→0→repeat (period 8)
    # Bottom dots: half-period offset (shift 4), so 3→2→1→0→0→1→2→3
    dots_cycle = ["", "◉", "◉ ◉", "◉ ◉ ◉", "◉ ◉ ◉", "◉ ◉", "◉", ""]
    half_period = len(dots_cycle) // 2  # 4

    def _draw_frame(stdscr, h, w, frame: int, state_val: int):
        """Draw a single loading frame: top dots, status text, bottom dots."""
        top_dots = dots_cycle[frame % len(dots_cycle)]
        bot_dots = dots_cycle[(frame + half_period) % len(dots_cycle)]

        stdscr.erase()

        # Top dots
        if top_dots:
            line = f" {top_dots} "
            x = max(0, (w - len(line)) // 2)
            safe_addstr(stdscr, h // 2 - 1, x, line, c_highlight)

        # Status text from LoadingState enum label
        state_label = _loading_state_label(state_val)
        state_line = f" {state_label} "
        x = max(0, (w - len(state_line)) // 2)
        safe_addstr(stdscr, h // 2, x, state_line, c_default)

        # Bottom dots
        if bot_dots:
            line = f" {bot_dots} "
            x = max(0, (w - len(line)) // 2)
            safe_addstr(stdscr, h // 2 + 1, x, line, c_highlight)

        stdscr.refresh()
        curses.doupdate()

    # Draw the first frame immediately and signal the data thread to proceed
    state = _get_loading_state()
    _draw_frame(stdscr, h, w, 0, state)
    last_state = state
    last_frame = 0

    # Signal the data thread that the animation is ready and rendering
    animation_ready.set()

    # Continue until both the data thread stops AND the queue is fully drained.
    # This ensures the final COMPLETE state is rendered before transitioning.
    while data_thread.is_alive() or not _loading_queue.empty():
        h, w = stdscr.getmaxyx()
        if h < 3 or w < 10:
            break

        # Drain all queued state changes. Each state emitted by reader.py is
        # enqueued, so even rapid sequential states (PROCESSING, CHECKING_PROPOSALS,
        # COMPLETE) are rendered one by one — none are lost.
        while True:
            try:
                state = _loading_queue.get_nowait()
            except queue.Empty:
                break
            _draw_frame(stdscr, h, w, int(time.time() * 2) % len(dots_cycle), state)
            last_state = state
            last_frame = int(time.time() * 2) % len(dots_cycle)
            # Small delay so each state is visible before the next one
            time.sleep(0.05)

        # Advance frame animation even when no state change (500ms per tick)
        frame = int(time.time() * 2) % len(dots_cycle)
        if frame != last_frame:
            _draw_frame(stdscr, h, w, frame, last_state)
            last_frame = frame

        time.sleep(0.1)

    # Thread finished; drain any remaining queued states so the final state
    # (e.g. COMPLETE) is rendered before transitioning to the data view.
    h, w = stdscr.getmaxyx()
    if h >= 3 and w >= 10:
        while True:
            try:
                state = _loading_queue.get_nowait()
            except queue.Empty:
                break
            _draw_frame(stdscr, h, w, int(time.time() * 2) % len(dots_cycle), state)

    # Force a clean screen transition so the main render loop gets a fresh buffer.
    stdscr.erase()
    stdscr.refresh()
    curses.doupdate()
    return False


def main(stdscr, preloaded_rows=None, preloaded_error=None):
    """Main curses loop.

    If *preloaded_rows* is provided, the Textual loading screen already fetched
    the data and the curses loading animation is skipped entirely.
    Otherwise, falls back to the curses loading animation.
    """
    log("main() entered, preloaded_rows: %s", "yes" if preloaded_rows is not None else "no")
    curses.curs_set(0)
    stdscr.nodelay(False)

    no_color = os.environ.get("NO_COLOR", "").strip() != ""
    config = load_config()
    is_light_theme = config.get("theme") == "light"
    log("Config loaded, theme: %s, light: %s", config.get("theme"), is_light_theme)

    # Initialize colors early — required whether or not _show_loading() runs.
    # When Textual preloads data, _show_loading() is skipped, so start_color()
    # must be called here to avoid a blank screen.
    if curses.has_colors() and not no_color:
        curses.start_color()
        init_colors(is_light_theme, no_color)

    if preloaded_rows is not None:
        # Data was already loaded by the Textual loading screen — skip animation
        rows = preloaded_rows
        load_error = preloaded_error
        log("Using preloaded rows: %d rows", len(rows) if rows else 0)
    else:
        # Fallback: curses loading animation (when Textual unavailable)
        _register_state_callback()

        load_result = [None, None]  # [rows, error]
        animation_ready = threading.Event()

        def _load_in_thread():
            animation_ready.wait(timeout=5)
            loaded_rows, error = load_data()
            if error:
                log_error("Data load error: %s", error)
            else:
                log("Data loaded: %d rows", len(loaded_rows) if loaded_rows else 0)
            load_result[0] = loaded_rows
            load_result[1] = error

        data_thread = threading.Thread(target=_load_in_thread, daemon=True)
        data_thread.start()

        _show_loading(stdscr, "Loading applications...", is_light_theme, no_color, data_thread, animation_ready)
        data_thread.join(timeout=30)

        rows, load_error = load_result
        if load_error:
            log_error("Load error after animation: %s", load_error)

    # Start background refresh thread for reactive updates
    start_background_refresh()
    log("Background refresh started")

    # After the blocking Firestore fetch, force the curses buffer to flush.
    # Ghostty's render pipeline can skip the first draw after a long blocking
    # call — doupdate() flushes the window tree, then erase() + refresh()
    # gives the main render loop a clean slate so the first frame lands.
    curses.doupdate()
    stdscr.erase()
    stdscr.refresh()
    curses.doupdate()

    cols_config = config["columns"]
    col_widths = get_column_widths(rows, cols_config)
    
    sort_idx = 0
    sort_dirs = {c["key"]: (c["key"] == 'date' or c["key"] == 'fit_rating') for c in cols_config}
    
    def sort_data():
        visible_cols = [c for c in cols_config if c.get("visible", True)]
        if not visible_cols: return
        col = visible_cols[sort_idx % len(visible_cols)]["key"]
        desc = sort_dirs.get(col, False)
        def sort_key(x):
            val = x.get(col, '') or ""
            if col == 'fit_rating':
                try: return int(val)
                except: return -1 if desc else 1000000
            if not val: return "" if not desc else "zzzzzz"
            return val.lower()
        rows.sort(key=sort_key, reverse=desc)
    
    sort_data()
    
    current_row = 0
    list_scroll = 0
    
    in_detail = False
    in_settings = False
    in_file_view = False
    settings_idx = 0
    
    active_selection_idx = None
    focused_pane = "LIST" # "LIST" or "DETAILS"
    detail_lines = []
    detail_scroll = 0
    detail_idx = 0
    
    # Cache detail lines by row id so they are only loaded once.
    # Cleared on background refresh in case file paths change.
    detail_cache: dict[str, list] = {}
    
    file_view_lines = []
    file_view_scroll = 0
    file_view_title = ""

    while True:
        stdscr.clear()
        h, w = stdscr.getmaxyx()
        
        # Guard against zero-sized windows (resize edge case)
        if h < 3 or w < 10:
            stdscr.refresh()
            stdscr.getch()
            continue
        
        c_header = curses.color_pair(1) | curses.A_BOLD if curses.has_colors() else curses.A_REVERSE
        c_selected = curses.color_pair(2) if curses.has_colors() else curses.A_REVERSE
        c_highlight = curses.color_pair(3) | curses.A_BOLD if curses.has_colors() else curses.A_BOLD
        c_footer = curses.color_pair(1) if curses.has_colors() else curses.A_REVERSE
        c_default = curses.color_pair(4) if curses.has_colors() else 0

        if in_file_view:
            # --- FILE VIEW LAYER ---
            header_text = f" VIEWING FILE | {file_view_title} "
            safe_addstr(stdscr, 0, 0, header_text.ljust(w)[:w-1], c_header)
            
            max_disp = h - 3
            for i in range(max_disp):
                idx = file_view_scroll + i
                if idx >= len(file_view_lines): break
                safe_addstr(stdscr, 2 + i, 1, file_view_lines[idx][:w-2], c_default)
            
            progress = int((file_view_scroll / max(1, len(file_view_lines) - max_disp)) * 100) if len(file_view_lines) > max_disp else 100
            footer_text = f" [j/k]: Scroll | [q, ESC]: Back | {progress}% "
            safe_addstr(stdscr, h - 1, 0, footer_text.ljust(w)[:w-1], c_footer)

        elif in_settings:
            # --- SETTINGS VIEW ---
            header_text = " TUI SETTINGS | [Space]: Toggle | [u/d]: Move | [Enter]: Save | [Esc]: Cancel "
            safe_addstr(stdscr, 0, 0, header_text.ljust(w)[:w-1], c_header)
            
            options = cols_config
            for i, cfg in enumerate(options):
                prefix = "[x]" if cfg.get("visible", True) else "[ ]"
                line = f" {i+1}. {prefix} {cfg['label']} ({cfg['key']})"
                if i == settings_idx:
                    safe_addstr(stdscr, 2 + i, 2, line.ljust(w-4), c_selected)
                else:
                    safe_addstr(stdscr, 2 + i, 2, line, c_default)
            
            footer_text = f" Theme: {'Light' if is_light_theme else 'Dark'} | Split-View: {'ON' if config['split_mode'] else 'OFF'} "
            safe_addstr(stdscr, h - 1, 0, footer_text.ljust(w)[:w-1], c_footer)

        elif in_detail and not config["split_mode"]:
            # --- FULL-SCREEN DETAIL VIEW ---
            header_text = f" DETAILS | {rows[current_row].get('company')} - {rows[current_row].get('role')} "
            safe_addstr(stdscr, 0, 0, header_text.ljust(w)[:w-1], c_header)
            
            max_disp = h - 3
            if detail_idx < detail_scroll: detail_scroll = detail_idx
            elif detail_idx >= detail_scroll + max_disp: detail_scroll = detail_idx - max_disp + 1

            for i in range(max_disp):
                idx = detail_scroll + i
                if idx >= len(detail_lines): break
                line_text, fpath = detail_lines[idx]
                line_text = line_text[:w-2]
                
                attr = c_default
                if line_text.startswith("===") or line_text.startswith("COMPANY:") or line_text.startswith("ROLE:"):
                    attr = c_highlight
                if idx == detail_idx:
                    attr = c_selected
                elif fpath:
                    attr = c_highlight
                
                safe_addstr(stdscr, 2 + i, 1, line_text, attr)
            
            progress = int((detail_scroll / max(1, len(detail_lines) - max_disp)) * 100) if len(detail_lines) > max_disp else 100
            footer_text = f" [j/k]: Nav | [Enter]: View File | [b, ESC]: Back | {progress}% "
            safe_addstr(stdscr, h - 1, 0, footer_text.ljust(w)[:w-1], c_footer)
            
        else:
            # --- LIST / SPLIT VIEW ---
            is_split = config["split_mode"] and active_selection_idx is not None
            list_w = (w // 2) if is_split else w
            det_w = (w - list_w - 1) if is_split else 0
            
            visible_cols = [c for c in cols_config if c.get("visible", True)]
            
            # Show error message if data load failed
            if load_error is not None:
                err_lines = textwrap.wrap(f"ERROR: {load_error}", width=min(w - 4, 120))
                for i, line in enumerate(err_lines):
                    if i + 2 < h - 1:
                        safe_addstr(stdscr, 2 + i, 2, line, c_highlight)
                # Still show footer so quit works
                footer_text = " [q]: Quit "
                safe_addstr(stdscr, h - 1, 0, footer_text.ljust(w)[:w - 1], c_footer)
                stdscr.refresh()
                key = stdscr.getch()
                if key in [ord('q'), 27]:
                    break
                continue
            
            # Check for background refresh data
            new_rows, new_error = check_background_refresh()
            if new_rows is not None:
                if new_error is None:
                    rows = new_rows
                    col_widths = get_column_widths(rows, cols_config)
                    sort_data()
                    # Reset scroll/selection after refresh
                    list_scroll = 0
                    current_row = 0
                    # Invalidate detail cache — file paths or metadata may have changed
                    detail_cache.clear()
                # Don't show error for background refresh failures —
                # the initial load error (if any) is still the authoritative one

            # Header
            header_text = f" AI JOB TRACKER | {len(rows)} Apps "
            if is_split: header_text += f"| Focus: {focused_pane} "
            # Provenance is part of the header, not a hidden status: a stale
            # cache must never be mistaken for the live record.
            header_text += f"| {DATA_META.get('label', 'unknown')} "
            # Show refresh indicator if background thread is active
            if _refresh_thread is not None and _refresh_thread.is_alive():
                header_text += "| (auto-refresh) "
            pending = DATA_META.get("pendingProposals") or 0
            if pending:
                header_text += f"| {pending} to review "
            safe_addstr(stdscr, 0, 0, header_text.ljust(list_w)[:list_w], c_header)
            
            # Column fitting
            total_fixed = 0
            calc_widths = []
            for cfg in visible_cols:
                if cfg["key"] == "role":
                    calc_widths.append(0)
                else:
                    cw = col_widths[cfg["key"]]
                    calc_widths.append(cw)
                    total_fixed += cw + 3
            
            role_w = list_w - total_fixed - 2
            for i, cfg in enumerate(visible_cols):
                if cfg["key"] == "role": calc_widths[i] = max(0, role_w)
            
            # Column headers
            col_parts = []
            for i, cfg in enumerate(visible_cols):
                arrow = "v" if sort_dirs.get(cfg["key"]) else "^"
                label = f"[{i+1}] {cfg['label']} {arrow if len(visible_cols) > 0 and (sort_idx % len(visible_cols)) == i else ''}"
                col_parts.append(f"{label[:calc_widths[i]]:<{calc_widths[i]}}")
            
            header_line = " " + " | ".join(col_parts)
            safe_addstr(stdscr, 2, 0, header_line[:list_w], c_highlight)
            safe_addstr(stdscr, 3, 0, ("-" * list_w)[:list_w], c_default)
            
            max_disp = h - 6
            if current_row < list_scroll: list_scroll = current_row
            elif current_row >= list_scroll + max_disp: list_scroll = current_row - max_disp + 1
                
            if not rows:
                safe_addstr(stdscr, 4, 2, "No applications found.", c_highlight)
                if DATA_META.get("source") == "unavailable":
                    safe_addstr(stdscr, 5, 2, "Could not connect to data source.", c_highlight)
            else:
                for i in range(max_disp):
                    idx = list_scroll + i
                    if idx >= len(rows): break
                    row = rows[idx]
                    row_parts = []
                    for j, cfg in enumerate(visible_cols):
                        val = str(row.get(cfg["key"], "") or "")
                        if cfg["key"] == "fit_rating" and not val:
                            val = "?? RECALC"
                        width = calc_widths[j]
                        row_parts.append(f"{val[:width]:<{width}}")
                    
                    line = " " + " | ".join(row_parts)
                    attr = c_selected if idx == current_row else (c_highlight if idx == active_selection_idx else c_default)
                    safe_addstr(stdscr, 4 + i, 0, line.ljust(list_w)[:list_w], attr)
            
            footer_text = " [j/k]: Nav | [Enter]: Detail | [r]: Recalc | [Tab]: Focus | [c]: Config | [t]: Theme | [q]: Quit "
            safe_addstr(stdscr, h - 1, 0, footer_text.ljust(w)[:w-1], c_footer)
            
            if is_split:
                for i in range(1, h - 1): safe_addstr(stdscr, i, list_w, "|", c_default)
                sel_row = rows[active_selection_idx] if active_selection_idx is not None else {}
                det_company = sel_row.get("company") or "Unknown"
                det_header = f" DETAILS: {det_company} | [Enter]: View File "
                safe_addstr(stdscr, 0, list_w+1, det_header.ljust(det_w)[:det_w], c_header if focused_pane=="DETAILS" else c_highlight)
                
                max_det_disp = h - 3
                if detail_idx < detail_scroll: detail_scroll = detail_idx
                elif detail_idx >= detail_scroll + max_det_disp: detail_scroll = detail_idx - max_det_disp + 1

                for i in range(max_det_disp):
                    idx = detail_scroll + i
                    if idx >= len(detail_lines): break
                    line_text, fpath = detail_lines[idx]
                    
                    attr = c_default
                    if idx == detail_idx and focused_pane == "DETAILS":
                        attr = c_selected
                    elif fpath:
                        attr = c_highlight

                    safe_addstr(stdscr, 2+i, list_w+2, line_text[:det_w-2], attr)

        stdscr.refresh()
        key = stdscr.getch()
        
        if key == ord('q'):
            if in_file_view:
                in_file_view = False
                continue
            else:
                break
        if key == 27: # ESC
            if in_file_view:
                in_file_view = False
            elif in_settings:
                in_settings = False
            elif in_detail:
                in_detail = False
            elif active_selection_idx is not None:
                active_selection_idx = None
                detail_lines = []
                detail_scroll = 0
                detail_idx = 0
                continue

        if in_file_view:
            if key in [curses.KEY_DOWN, ord('j')]: file_view_scroll = min(len(file_view_lines)-(h-3), file_view_scroll+1)
            elif key in [curses.KEY_UP, ord('k')]: file_view_scroll = max(0, file_view_scroll-1)
        elif in_settings:
            if key in [curses.KEY_DOWN, ord('j')] and cols_config:
                settings_idx = (settings_idx + 1) % len(cols_config)
            elif key in [curses.KEY_UP, ord('k')] and cols_config:
                settings_idx = (settings_idx - 1) % len(cols_config)
            elif key == ord(' ') and cols_config:
                cols_config[settings_idx]["visible"] = not cols_config[settings_idx].get("visible", True)
            elif key == ord('u') and settings_idx > 0 and cols_config:
                cols_config[settings_idx], cols_config[settings_idx-1] = cols_config[settings_idx-1], cols_config[settings_idx]
                settings_idx -= 1
            elif key == ord('d') and settings_idx < len(cols_config) - 1 and cols_config:
                cols_config[settings_idx], cols_config[settings_idx+1] = cols_config[settings_idx+1], cols_config[settings_idx]
                settings_idx += 1
            elif key in [10, 13]:
                config["columns"] = cols_config
                save_config(config)
                in_settings = False
        elif in_detail and not config["split_mode"]:
            if key == ord('r') and rows:
                try:
                    target_idx = active_selection_idx if active_selection_idx is not None else current_row
                    target_idx = max(0, min(target_idx, len(rows) - 1))
                    write_recalc_request(rows[target_idx])
                except Exception as e:
                    log_error("Recalc request failed (detail view): %s", e)
                log("Recalc request queued (detail view)")
                continue
            if key in [ord('b'), ord('q')]: in_detail = False
            elif key in [curses.KEY_DOWN, ord('j')]:
                detail_idx = min(max(0, len(detail_lines)-1), detail_idx+1)
            elif key in [curses.KEY_UP, ord('k')]:
                detail_idx = max(0, detail_idx-1)
            elif key in [10, 13] and detail_lines:
                _, fpath = detail_lines[detail_idx]
                if fpath:
                    if fpath.startswith('gs://') or '/' in fpath and not os.path.isabs(fpath):
                        # GCS path — for .md files, download and render inline
                        detail_text = detail_lines[detail_idx][0]
                        filename_from_detail = detail_text.split(":")[-1].strip() if ":" in detail_text else "file"
                        if filename_from_detail.lower().endswith('.md'):
                            try:
                                sys.path.insert(0, str(REPO_ROOT / "scripts"))
                                import client as client_mod  # noqa: PLC0415
                                import artifacts as artifacts_mod  # noqa: PLC0415
                                bucket = client_mod.getBucket()
                                content = artifacts_mod.download_text(bucket, fpath)
                                file_view_title = filename_from_detail
                                file_view_lines = content.splitlines()
                                in_file_view = True
                                file_view_scroll = 0
                            except Exception as e:
                                file_view_lines = [f"Error downloading from GCS: {e}"]
                                in_file_view = True
                        else:
                            # Generate signed URL and open in browser
                            try:
                                sys.path.insert(0, str(REPO_ROOT / "scripts"))
                                import client as client_mod  # noqa: PLC0415
                                import artifacts as artifacts_mod  # noqa: PLC0415
                                import webbrowser
                                bucket = client_mod.getBucket()
                                url = artifacts_mod.signed_url(bucket, fpath)
                                webbrowser.open(url)
                            except Exception as e:
                                file_view_lines = [f"Error generating GCS URL: {e}"]
                                in_file_view = True
                    elif fpath.startswith("http"):
                        import webbrowser
                        webbrowser.open(fpath)
                    else:
                        file_view_title = os.path.basename(fpath)
                        try:
                            if fpath.lower().endswith('.pdf'):
                                file_view_lines = ["[Binary PDF File - Cannot display content in TUI]", f"Path: {fpath}"]
                            else:
                                with open(fpath, 'r', errors='replace') as f:
                                    file_view_lines = f.read().splitlines()
                            in_file_view = True
                            file_view_scroll = 0
                        except Exception as e:
                            file_view_lines = [f"Error reading file: {e}"]
                            in_file_view = True
        else:
            if key == ord('c'): in_settings = True
            elif key == ord('r') and rows:
                try:
                    target_idx = active_selection_idx if active_selection_idx is not None else current_row
                    target_idx = max(0, min(target_idx, len(rows) - 1))
                    write_recalc_request(rows[target_idx])
                except Exception as e:
                    log_error("Recalc request failed (main view): %s", e)
                log("Recalc request queued (main view)")
                continue
            elif key == ord('t'):
                is_light_theme = not is_light_theme
                config["theme"] = "light" if is_light_theme else "dark"
                init_colors(is_light_theme, no_color)
                stdscr.bkgd(' ', curses.color_pair(4))
                save_config(config)
            elif key == ord('\t'):
                if active_selection_idx is not None: focused_pane = "DETAILS" if focused_pane == "LIST" else "LIST"
            elif key in [curses.KEY_DOWN, ord('j')]:
                if focused_pane == "LIST" and rows:
                    current_row = min(len(rows)-1, current_row+1)
                elif detail_lines:
                    detail_idx = min(max(0, len(detail_lines)-1), detail_idx+1)
            elif key in [curses.KEY_UP, ord('k')]:
                if focused_pane == "LIST" and rows:
                    current_row = max(0, current_row-1)
                elif detail_lines:
                    detail_idx = max(0, detail_idx-1)
            elif key in [ord('1'), ord('2'), ord('3'), ord('4'), ord('5')]:
                v_cols = [c for c in cols_config if c.get("visible", True)]
                idx = (key - ord('1'))
                if idx < len(v_cols):
                    sort_idx = idx
                    k = v_cols[idx]["key"]
                    sort_dirs[k] = not sort_dirs.get(k, False)
                    sort_data()
            elif key in [10, 13]:
                if not rows:
                    continue
                if config["split_mode"]:
                    if focused_pane == "DETAILS" and detail_lines:
                        _, fpath = detail_lines[detail_idx]
                        if fpath:
                            if fpath.startswith('gs://') or '/' in fpath and not os.path.isabs(fpath):
                                # GCS path — for .md files, download and render inline
                                detail_text = detail_lines[detail_idx][0]
                                filename_from_detail = detail_text.split(":")[-1].strip() if ":" in detail_text else "file"
                                if filename_from_detail.lower().endswith('.md'):
                                    try:
                                        sys.path.insert(0, str(REPO_ROOT / "scripts"))
                                        import client as client_mod  # noqa: PLC0415
                                        import artifacts as artifacts_mod  # noqa: PLC0415
                                        bucket = client_mod.getBucket()
                                        content = artifacts_mod.download_text(bucket, fpath)
                                        file_view_title = filename_from_detail
                                        file_view_lines = content.splitlines()
                                        in_file_view = True
                                        file_view_scroll = 0
                                    except Exception as e:
                                        file_view_lines = [f"Error downloading from GCS: {e}"]
                                        in_file_view = True
                                else:
                                    # Generate signed URL and open in browser
                                    try:
                                        sys.path.insert(0, str(REPO_ROOT / "scripts"))
                                        import client as client_mod  # noqa: PLC0415
                                        import artifacts as artifacts_mod  # noqa: PLC0415
                                        import webbrowser
                                        bucket = client_mod.getBucket()
                                        url = artifacts_mod.signed_url(bucket, fpath)
                                        webbrowser.open(url)
                                    except Exception as e:
                                        file_view_lines = [f"Error generating GCS URL: {e}"]
                                        in_file_view = True
                            elif fpath.startswith("http"):
                                import webbrowser
                                webbrowser.open(fpath)
                            else:
                                file_view_title = os.path.basename(fpath)
                                try:
                                    if fpath.lower().endswith('.pdf'):
                                        file_view_lines = ["[Binary PDF File - Cannot display content in TUI]", f"Path: {fpath}"]
                                    else:
                                        with open(fpath, 'r', errors='replace') as f:
                                            file_view_lines = f.read().splitlines()
                                    in_file_view = True
                                    file_view_scroll = 0
                                except Exception as e:
                                    file_view_lines = [f"Error reading file: {e}"]
                                    in_file_view = True
                    else:
                        if active_selection_idx == current_row:
                            active_selection_idx = None
                            focused_pane = "LIST"
                            detail_lines = []
                            detail_scroll = 0
                            detail_idx = 0
                        else:
                            active_selection_idx = current_row
                            row_id = rows[active_selection_idx].get('id', '')
                            if row_id in detail_cache:
                                detail_lines = detail_cache[row_id]
                            else:
                                detail_lines = load_detail_lines(rows[active_selection_idx], w//2 - 2)
                                detail_cache[row_id] = detail_lines
                            detail_scroll = 0
                            detail_idx = 0
                else:
                    row_id = rows[current_row].get('id', '')
                    if row_id in detail_cache:
                        detail_lines = detail_cache[row_id]
                    else:
                        detail_lines = load_detail_lines(rows[current_row], w - 2)
                        detail_cache[row_id] = detail_lines
                    in_detail = True
                    detail_scroll = 0
                    detail_idx = 0

if __name__ == "__main__":
    # Initialize persistent logging before anything else
    _setup_logging()
    log("TUI starting, textual available: %s", TEXTUAL_AVAILABLE)

    # Only use the Textual loading screen when we have a real TTY.
    # Textual and curses cannot share the terminal, and the handoff
    # requires termios to save/restore the tty attributes — which only
    # works with a real terminal device. Without a TTY, fall back to
    # the curses-only path with its own loading animation.
    use_textual = TEXTUAL_AVAILABLE and os.isatty(sys.stdin.fileno())

    cancelled = False
    if use_textual:
        # Textual loading screen → curses table handoff
        rows, load_error = run_textual_loading()
        # If the user cancelled the loading screen, exit cleanly
        if load_error == "Cancelled by user":
            cancelled = True
            log("Loading cancelled by user")
        else:
            preloaded = True
            if load_error:
                log_error("Load error from textual: %s", load_error)
            else:
                log("Textual loading completed, rows loaded")
    else:
        # Curses-only path — main() will show the loading animation
        rows = None
        load_error = None
        preloaded = False
        log("Textual not available, using curses-only path")

    if not cancelled:
        try:
            if preloaded:
                curses.wrapper(lambda stdscr: main(stdscr, rows, load_error))
            else:
                curses.wrapper(main)
        except KeyboardInterrupt:
            log("TUI interrupted by user (Ctrl+C)")
        except Exception as e:
            log_crash(e)
            error_msg = f"\nTUI crashed with an error.\nCrash log: {crash_log_file_path}"
            print(error_msg, file=sys.stderr)
            traceback.print_exc()
        finally:
            stop_background_refresh()
            log("TUI shutdown complete")
