package cn.frkovo.rhythmcv2.rmcChart.chart.core;

public final class JudgeMath {
    public static final long JUDGE_T_PERFECT_MILLIS = 110L;
    public static final long JUDGE_T_GREAT_MILLIS = 220L;

    private JudgeMath() {
    }

    public static JudgeResult judgeTap(long noteDiffMillis) {
        if (noteDiffMillis >= JUDGE_T_GREAT_MILLIS) {
            return JudgeResult.FAST_GREAT;
        }
        if (noteDiffMillis <= -JUDGE_T_PERFECT_MILLIS) {
            return JudgeResult.LATE_GREAT;
        }
        if (noteDiffMillis >= JUDGE_T_PERFECT_MILLIS) {
            return JudgeResult.FAST_GREAT;
        }
        return JudgeResult.PERFECT;
    }

    public static JudgeResult judgeLookHold(long noteDiffMillis) {
        if (noteDiffMillis <= -JUDGE_T_PERFECT_MILLIS) {
            return JudgeResult.LATE_GREAT;
        }
        return JudgeResult.PERFECT;
    }

    public static boolean isMiss(long noteDiffMillis) {
        return noteDiffMillis < -JUDGE_T_GREAT_MILLIS;
    }
}
