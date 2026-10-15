"""Unit tests of the parsing and comparison in verify_designer_performance.py (no device needed).

Run with: python -m unittest discover -s .python/tests
"""
import json
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
import verify_designer_performance as perf  # noqa: E402

LOGCAT = """
10-15 22:30:01.123  1234  1234 I DesignerPerformance: DESIGNER_PERF {"kind": "pipeline", "device": "Xiaomi 22120RN86C", "api": "33", "gallery_examples": "56", "gallery_read": "p50=33.0 p90=41.9 max=44.8 n=10", "large_read": "p50=6.0 p90=6.9 max=7.4 n=10"}
10-15 22:30:02.456  1234  1234 I DesignerPerformance: DESIGNER_PERF {"kind": "edit", "device": "Xiaomi 22120RN86C", "api": "33", "undo_restores_row": "p50=336.0 p90=378.5 max=404.2 n=10", "redo_removes_row": "p50=305.5 p90=359.5 max=368.6 n=10"}
10-15 22:30:03.789  1234  1234 I DesignerPerformance: not a record
"""

LINES = [
    {"key": "gallery_read", "title": "Gallery read (56 scripts)", "line_ms": 65},
    {"key": "large_read", "title": "Large read (251 nodes)", "line_ms": 15},
    {"key": "undo_restores_row", "title": "Undo restores a row (visibility)", "line_ms": 570},
    {"key": "redo_main_cpu", "title": "Redo: main thread CPU", "line_ms": 640},
]


class ParseTest(unittest.TestCase):
    def test_records_are_grouped_by_kind_with_their_numbers(self):
        records = perf.parse_perf_lines(LOGCAT)
        self.assertEqual({"pipeline", "edit"}, set(records))
        self.assertEqual("Xiaomi 22120RN86C", records["pipeline"]["device"])
        self.assertEqual((33.0, 41.9, 44.8, 10), records["pipeline"]["values"]["gallery_read"])
        self.assertEqual((305.5, 359.5, 368.6, 10), records["edit"]["values"]["redo_removes_row"])
        self.assertNotIn("gallery_examples", records["pipeline"]["values"])

    def test_the_committed_lines_file_is_consistent(self):
        data = json.loads(perf.DEFAULT_LINES.read_text(encoding="utf-8"))
        self.assertEqual("22120RN86C", data["device_model"])
        keys = [entry["key"] for entry in data["lines"]]
        self.assertEqual(16, len(keys))
        self.assertEqual(len(keys), len(set(keys)))
        for entry in data["lines"]:
            # The rule of D54: p90 plus 50 %, rounded up to 5 ms.
            expected = int(-(-entry["p90_ms"] * 1.5 // 5) * 5)
            self.assertEqual(expected, entry["line_ms"], entry["key"])
            self.assertTrue(entry["line_ms"] > entry["p90_ms"], entry["key"])


class CompareTest(unittest.TestCase):
    def measured(self):
        records = perf.parse_perf_lines(LOGCAT)
        measured = {}
        for record in records.values():
            measured.update(record["values"])
        return measured

    def test_judged_runs_pass_over_or_miss(self):
        measured = self.measured()
        measured["redo_main_cpu"] = (500.0, 700.0, 720.0, 10)
        rows, ok = perf.compare(measured, LINES, judge=True)
        self.assertEqual(["PASS", "PASS", "PASS", "OVER"], [status for status, *_ in rows])
        self.assertFalse(ok)
        del measured["redo_main_cpu"]
        rows, ok = perf.compare(measured, LINES, judge=True)
        self.assertEqual("MISSING", rows[-1][0])
        self.assertFalse(ok)
        measured["redo_main_cpu"] = (400.0, 640.0, 650.0, 10)
        rows, ok = perf.compare(measured, LINES, judge=True)
        self.assertTrue(ok)

    def test_other_devices_are_recorded_only(self):
        measured = self.measured()
        measured["redo_main_cpu"] = (500.0, 9000.0, 9100.0, 10)
        rows, ok = perf.compare(measured, LINES, judge=False)
        self.assertEqual({"RECORD"}, {status for status, *_ in rows})
        self.assertTrue(ok)

    def test_record_table_lists_every_line(self):
        table = perf.record_table(LINES, self.measured())
        self.assertIn("| Gallery read (56 scripts) | 33.0 / 41.9 | 65 ms |", table)
        self.assertIn("| Redo: main thread CPU | not measured | 640 ms |", table)


if __name__ == "__main__":
    unittest.main()
