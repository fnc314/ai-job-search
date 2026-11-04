from __future__ import annotations

import pathlib
import sys
import unittest

ROOT = pathlib.Path(__file__).resolve().parent.parent.parent
if str(ROOT / "scripts") not in sys.path:
    sys.path.insert(0, str(ROOT / "scripts"))

import fit_evaluator


class TestFitEvaluator(unittest.TestCase):
    def test_capital_one_mobile_engineer_4_explicit_calibration(self):
        app = {
            "company": "Capital One",
            "role": "Mobile Engineer 4, Android",
            "location": {"city": "Remote"},
            "jobUrl": "https://capitalone.wd12.myworkdayjobs.com/Capital_One/job/McLean-VA/Mobile-Engineer-4---Android--Remote-Eligible-_R1001320-1",
        }
        res = fit_evaluator.evaluate_fit(app)
        self.assertEqual(res["score"], 95)
        self.assertIn("Strong Fit", res["recommendation"])

    def test_principal_staff_android_role_scores_high(self):
        app = {
            "company": "Stripe",
            "role": "Staff Android Engineer",
            "location": {"city": "Remote"},
        }
        res = fit_evaluator.evaluate_fit(app)
        self.assertGreaterEqual(res["score"], 90)

    def test_deal_breakers_penalize_score(self):
        app_hybrid = {
            "company": "SomeStartup",
            "role": "React Native Mobile Developer",
            "location": {"city": "Remote"},
        }
        res = fit_evaluator.evaluate_fit(app_hybrid)
        self.assertLess(res["score"], 70)

        app_consulting = {
            "company": "BigConsultingFirm",
            "role": "Android Consultant",
            "location": {"city": "Remote"},
        }
        res_consulting = fit_evaluator.evaluate_fit(app_consulting)
        self.assertLess(res_consulting["score"], 70)

    def test_reddit_calibration_cap(self):
        app = {
            "company": "Reddit",
            "role": "Staff Android Engineer",
            "location": {"city": "Remote"},
        }
        res = fit_evaluator.evaluate_fit(app)
        self.assertLessEqual(res["score"], 75)


if __name__ == "__main__":
    unittest.main()
