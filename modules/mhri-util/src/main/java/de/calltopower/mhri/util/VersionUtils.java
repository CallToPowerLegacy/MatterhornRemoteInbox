/*
 * Copyright 2013-2015 Denis Meyer
 * All rights reserved.
 */
package de.calltopower.mhri.util;

/**
 * VersionTools
 *
 * @date 08.07.2013
 *
 * @author Denis Meyer (calltopower88@gmail.com)
 */
public class VersionUtils {

    /**
     * Returns the current version + build (formatted)
     *
     * @return the current version + build (formatted)
     */
    public String getCurrentVersion() {
        return "v" + Constants.MHRI_VERSION + " build " + Constants.MHRI_BUILD;
    }

    /**
     * Compares 2 versions (both formatted)
     *
     * @param v1 version 1
     * @param v2 version 2
     * @return 0 = equal, 1 = v1 > v2, -1 = v2 > v1, -10 = error
     */
    public int compareVersions(String v1, String v2) {
        if (v1.contains("alpha") || v2.contains("alpha") || v1.contains("beta") || v2.contains("beta") || v1.contains("release candidate") || v2.contains("release candidate")) {
            return 0;
        }
        char[] c1 = v1.toCharArray();
        char[] c2 = v2.toCharArray();
        if ((c1.length != c2.length) || (c1.length != 25)) {
            return -10;
        }
        // vx.y.z build ab.cd.efgh n
        // 012345 ...
        try {
            int x1 = Integer.parseInt(String.valueOf(c1[1]));
            int x2 = Integer.parseInt(String.valueOf(c2[1]));
            if (x1 > x2) {
                return 1;
            } else if (x1 < x2) {
                return -1;
            }
            int y1 = Integer.parseInt(String.valueOf(c1[3]));
            int y2 = Integer.parseInt(String.valueOf(c2[3]));
            if (y1 > y2) {
                return 1;
            } else if (y1 < y2) {
                return -1;
            }
            int z1 = Integer.parseInt(String.valueOf(c1[5]));
            int z2 = Integer.parseInt(String.valueOf(c2[5]));
            if (z1 > z2) {
                return 1;
            } else if (z1 < z2) {
                return -1;
            }
            int ab1 = Integer.parseInt(String.valueOf(c1, 13, 2));
            int ab2 = Integer.parseInt(String.valueOf(c2, 13, 2));
            if (ab1 > ab2) {
                return 1;
            } else if (ab1 < ab2) {
                return -1;
            }
            int cd1 = Integer.parseInt(String.valueOf(c1, 16, 2));
            int cd2 = Integer.parseInt(String.valueOf(c2, 16, 2));
            if (cd1 > cd2) {
                return 1;
            } else if (cd1 < cd2) {
                return -1;
            }
            int efgh1 = Integer.parseInt(String.valueOf(c1, 19, 4));
            int efgh2 = Integer.parseInt(String.valueOf(c2, 19, 4));
            if (efgh1 > efgh2) {
                return 1;
            } else if (efgh1 < efgh2) {
                return -1;
            }
            int n1 = Integer.parseInt(String.valueOf(c1[24]));
            int n2 = Integer.parseInt(String.valueOf(c2[24]));
            if (n1 > n2) {
                return 1;
            } else if (n1 < n2) {
                return -1;
            }
        } catch (NumberFormatException ex) {
            return -10;
        }

        return 0;
    }
}
