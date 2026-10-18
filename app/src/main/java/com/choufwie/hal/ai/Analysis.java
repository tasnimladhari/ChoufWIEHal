package com.choufwie.hal.ai;

/** Stage 1 output: structured signals extracted from the free-text check-in. */
public class Analysis {
    public int stress = 3;             // 1..10
    public String emotion = "calm";    // calm|hopeful|tired|anxious|sad|overwhelmed|angry
    public String stressor = "none";   // exams|workload|sleep|family_expectations|career|uncertainty|rejection|comparison|money|relationships|other|none
    public String event = "none";      // none|rejection|no_response|interview_done|offer|exam_done
}
