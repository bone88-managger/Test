import pytest
from lottery import Draw, load_csv, overlap_count, validate_candidate, generate

def sample_history():
    return [
        Draw("2025-01-01", (1, 2, 3, 4, 5), 6),
        Draw("2025-01-03", (10, 20, 30, 40, 50), 7),
    ]

def test_load_csv():
    rows = load_csv("date,white1,white2,white3,white4,white5,mega_ball\n2025-01-01,1,2,3,4,5,6\n")
    assert len(rows) == 1
    assert rows[0].white == (1, 2, 3, 4, 5)

def test_historical_eras_can_keep_values_above_current_ranges():
    old = Draw("2014-12-16", (41, 58, 68, 72, 73), 1)
    assert old.white[-1] == 73

def test_overlap_counts_unique_numeric_values_not_positions():
    assert overlap_count([1, 2, 3, 4, 5, 6], [1, 2, 8, 9, 10, 6]) == 3

def test_valid_candidate_at_most_two_shared_values():
    ok, _ = validate_candidate((11, 12, 13, 14, 15), 16, sample_history(), 2)
    assert ok

def test_reject_more_than_two_shared_values():
    ok, _ = validate_candidate((1, 2, 3, 14, 15), 6, sample_history(), 2)
    assert not ok

def test_mega_can_equal_white_number():
    ok, _ = validate_candidate((1, 2, 3, 4, 5), 1, [], 2)
    assert ok

def test_generation_respects_rules():
    result = generate(sample_history(), max_attempts=10000)
    assert len(set(result.white)) == 5
    assert all(1 <= n <= 70 for n in result.white)
    assert 1 <= result.mega <= 24
    assert all(validate_candidate(result.white, result.mega, [d], 2)[0] for d in sample_history())
