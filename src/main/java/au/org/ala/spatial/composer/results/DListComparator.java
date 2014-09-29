package au.org.ala.spatial.composer.results;

import java.util.Comparator;

/**
 * Created by a on 29/09/2014.
 */
class DListComparator implements Comparator {

    private boolean ascending;
    private boolean number;
    private int index;

    public DListComparator(boolean ascending, boolean number, int index) {
        this.ascending = ascending;
        this.number = number;
        this.index = index;
    }

    public int compare(Object o1, Object o2) {
        String[] s1 = (String[]) o1;
        String[] s2 = (String[]) o2;
        int sort;

        if (number) {
            Double d1 = null, d2 = null;
            try {
                d1 = Double.parseDouble(s1[index]);
            } catch (Exception e) {
                //using default null if it fails to parse
            }
            try {
                d2 = Double.parseDouble(s2[index]);
            } catch (Exception e) {
                //using default null if it fails to parse
            }
            if (d1 == null || d2 == null) {
                sort = (d1 == null ? 1 : 0) + (d2 == null ? -1 : 0);
            } else {
                sort = d1.compareTo(d2);
            }
        } else {
            String t1 = s1[index];
            String t2 = s2[index];
            if (t1 == null || t2 == null) {
                sort = (t1 == null ? 1 : 0) + (t2 == null ? -1 : 0);
            } else {
                sort = t1.compareTo(t2);
            }
        }

        return ascending ? sort : -sort;
    }
}
