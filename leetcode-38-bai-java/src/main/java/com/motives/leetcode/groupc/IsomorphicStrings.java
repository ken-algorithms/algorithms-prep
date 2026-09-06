package com.motives.leetcode.groupc;

import java.util.HashMap;
import java.util.Map;

/**
 * LeetCode #205 - Isomorphic Strings (Nhom C, bai 31).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class IsomorphicStrings {

    public boolean isIsomorphic(String s, String t) {
        Map<Character, Character> sourceToTarget = new HashMap<>();
        Map<Character, Character> targetToSource = new HashMap<>();

        for (int i = 0; i < s.length(); i++) {
            char sourceChar = s.charAt(i);
            char targetChar = t.charAt(i);

            Character mappedTarget = sourceToTarget.get(sourceChar);
            Character mappedSource = targetToSource.get(targetChar);

            if (mappedTarget != null && mappedTarget != targetChar) {
                return false;
            }
            if (mappedSource != null && mappedSource != sourceChar) {
                return false;
            }

            sourceToTarget.put(sourceChar, targetChar);
            targetToSource.put(targetChar, sourceChar);
        }
        return true;
    }
}
