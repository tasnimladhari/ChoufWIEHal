package com.choufwie.hal.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** All Room tables in one place. Public fields keep the rest of the code short. */
public final class Entities {
    private Entities() {}

    @Entity(tableName = "users", indices = {@Index(value = "username", unique = true)})
    public static class User {
        @PrimaryKey(autoGenerate = true) public long id;
        public String name;
        public String username;
        public String salt;
        public String passHash;
        public String mode;           // STUDENT | GRADUATE
        public long createdAt;
        public long jobSearchStart;   // 0 for students
    }

    /** Student: exam/project/thesis (weight = coefficient, grade /20). Graduate: interview/application/test/follow-up. */
    @Entity(tableName = "commitments", indices = {@Index("userId")})
    public static class Commitment {
        @PrimaryKey(autoGenerate = true) public long id;
        public long userId;
        public String title;
        public String type;
        public int weight = 3;        // 1..5
        public int difficulty = 3;    // 1..5
        public long dueDate;
        public double grade = -1;     // -1 = not graded / not done yet
        public int focusMinutes;
    }

    @Entity(tableName = "check_ins", indices = {@Index("userId")})
    public static class CheckIn {
        @PrimaryKey(autoGenerate = true) public long id;
        public long userId;
        public long timestamp;
        public String textEnc;        // AES-256-GCM, Base64
        public double sleepHours;
        public int energy;            // 1..5
        public boolean movement;
        public boolean water;
        public int stress;            // 1..10 (from the AI extraction)
        public String emotion = "calm";
        public String stressor = "none";
        public String event = "none";
    }

    @Entity(tableName = "wins", indices = {@Index("userId")})
    public static class Win {
        @PrimaryKey(autoGenerate = true) public long id;
        public long userId;
        public String day;            // ISO date
        public String text;
    }
}
