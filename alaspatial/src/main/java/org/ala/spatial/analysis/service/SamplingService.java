package org.ala.spatial.analysis.service;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

import org.ala.spatial.analysis.index.IndexedRecord;
import org.ala.spatial.analysis.index.OccurrencesIndex;
import org.ala.spatial.analysis.index.SamplingIndex;
import org.ala.spatial.util.Layers;
import org.ala.spatial.util.OccurrencesFieldsUtil;
import org.ala.spatial.util.SimpleRegion;
import org.ala.spatial.util.TabulationSettings;

/**
 * service for returning occurrences + optional values from layer intersections
 * 
 * @author adam
 *
 */
public class SamplingService {

    /**
     * constructor init
     */
    public SamplingService() {
        TabulationSettings.load();
    }

    /**
     * gets samples; occurrences records + optional intersecting layer values,
     * as csv
     *
     * @param filter species name as String
     * @param layers list of layer names of additional data to include as String []
     * @return samples as csv, String
     */
    public String sampleSpecies(String filter, String[] layers) {
        return sampleSpecies(filter, layers, null);
    }

    /**
     * gets samples; occurrences records + optional intersecting layer values,
     * as csv
     *
     * @param filter species name as String
     * @param layers list of layer names of additional data to include as String []
     * @param baseDir base directory where the file should be written to
     * @return file name for samples in csv format, String
     */
    public String sampleSpecies(String filter, String[] layers, File baseDir) {
        StringBuffer output = new StringBuffer();

        /* output header */
        OccurrencesFieldsUtil ofu = new OccurrencesFieldsUtil();

        for (String s : ofu.getOutputColumnNames()) {
            output.append(s);
            output.append(",");
        }
        if (layers != null) {
            for (String l : layers) {
                output.append(Layers.layerNameToDisplayName(l));
                output.append(",");
            }
        }

        /* tidy up header */
        output.deleteCharAt(output.length() - 1); //take off end ','
        output.append("\r\n");

        IndexedRecord[] ir = OccurrencesIndex.filterSpeciesRecords(filter);
        int i, j;

        if (ir != null && layers != null && layers.length > 0) {
            ArrayList<String[]> columns = new ArrayList<String[]>(layers.length + 1);

            try {
                File temporary_file = null;
                if (baseDir == null) {
                    temporary_file = File.createTempFile("sample", ".csv");
                } else {
                    temporary_file = File.createTempFile("sample", ".csv", baseDir);
                }
                FileWriter fw = new FileWriter(temporary_file);

                fw.append(output.toString());

                for (IndexedRecord r : ir) {
                    columns.clear();

                    /*
                     * cap the number of records per read
                     */

                    int step = 5000000; //max characters to read
                    int rstart = r.file_start;
                    int rend;

                    rend = rstart + step;
                    if (rend > r.file_end) {
                        rend = r.file_end;
                    }

                    String[] sortedrecords;
                    int recordstart = r.record_start;
                    int recordend;

                    String lastpart = "";

                    while (rend <= r.file_end) {

                        columns.clear();

                        sortedrecords = OccurrencesIndex.getSortedRecords(rstart, rend);
                        sortedrecords[0] = lastpart + sortedrecords[0];

                        columns.add(sortedrecords);

                        if (rend == r.file_end) {
                            //do all records
                            recordend = r.record_end;
                            lastpart = "";
                        } else {
                            //do up to last record
                            recordend = recordstart + sortedrecords.length - 2;
                            lastpart = sortedrecords[sortedrecords.length - 1];

                        }

                        for (i = 0; i < layers.length; i++) {
                            columns.add(SamplingIndex.getRecords(
                                    Layers.layerDisplayNameToName(layers[i]),
                                    recordstart,
                                    recordend));
                        }

                        /* format output records */
                        int len = columns.get(1).length;
                        for (j = 0; j < len; j++) {
                            for (i = 0; i < columns.size(); i++) {
                                if (columns.get(i) != null && j < columns.get(i).length) {
                                    if (!(columns.get(i)[j] == null) && !columns.get(i)[j].equals("NaN")) {
                                        fw.append(columns.get(i)[j]);
                                    }
                                    if (i < columns.size() - 1) {
                                        fw.append(",");
                                    }
                                }
                            }

                            fw.append("\r\n");
                        }

                        /* adjust for next loop */
                        recordstart = recordend + 1; 		//this was inclusive
                        if (rend < r.file_end) {
                            rstart = rend;				//this is not inclusive
                            rend = rstart + step;
                            if (rend > r.file_end) {
                                rend = r.file_end;
                            }
                        } else {
                            rend = r.file_end + 1;
                        }
                    }
                }

                fw.close();
                return temporary_file.getPath();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (ir != null) {
            try {
                File temporary_file = java.io.File.createTempFile("sample", ".csv");
                FileWriter fw = new FileWriter(temporary_file);

                fw.append(output.toString());

                for (IndexedRecord r : ir) {

                    int step = 10000000;		/*TODO: move this into tabulation_settings.xml */

                    for (i = r.file_start; i < r.file_end - step; i += step) {
                        fw.append(OccurrencesIndex.getSortedRecordsString(
                                i, i + step));
                    }

                    fw.append(OccurrencesIndex.getSortedRecordsString(
                            i, r.file_end));

                }
                fw.close();
                return temporary_file.getPath();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * gets samples; occurrences records + optional intersecting layer values,
     *
     *
     * limit output
     *
     * @param filter species name as String
     * @param layers list of layer names of additional data to include as String []
     * @param region region to restrict results as SimpleRegion
     * @param records sorted pool of records to intersect with as ArrayList<Integer>
     * @param max_rows upper limit of records to return as int
     * @return samples as grid, String [][]
     */
    public String sampleSpeciesAsCSV(String filter, String[] layers, SimpleRegion region, ArrayList<Integer> records, int max_rows) {
        try {

            System.out.println("Limiting sampling to : " + max_rows); 

            String[][] results = sampleSpecies(filter, layers, region, null, max_rows);
            StringBuilder sbResults = new StringBuilder();

            for (int i = 0; i < results.length; i++) {
                for (int j = 0; j < results[i].length; j++) {
                    if (results[i][j] != null) {
                        sbResults.append(results[i][j]);
                    }
                    if (j < results[i].length - 1) {
                        sbResults.append(",");
                    }
                }
                sbResults.append("\r\n");
            }

            /* open output file */
            File temporary_file = java.io.File.createTempFile("sample", ".csv");
            FileWriter fw = new FileWriter(temporary_file);

            fw.append(sbResults.toString());
            fw.close();
            return temporary_file.getPath();


        } catch (Exception e) {
            System.out.println("error with samplesSpeciesAsCSV:");
            e.printStackTrace(System.out);
        }

        return "";
    }

    /**
     * gets samples; occurrences records + optional intersecting layer values,
     *
     *
     * limit output
     *
     * @param filter species name as String
     * @param layers list of layer names of additional data to include as String []
     * @param region region to restrict results as SimpleRegion
     * @param records sorted pool of records to intersect with as ArrayList<Integer>
     * @param max_rows upper limit of records to return as int
     * @return samples as grid, String [][]
     */
    public String[][] sampleSpecies(String filter, String[] layers, SimpleRegion region, ArrayList<Integer> records, int max_rows) {
        String[][] results = null;

        StringBuffer output = new StringBuffer();

        int number_of_columns = TabulationSettings.occurances_csv_fields.length;

        OccurrencesFieldsUtil ofu = new OccurrencesFieldsUtil();

        for (String s : ofu.getOutputColumnNames()) {
            output.append(s);
            output.append(",");
        }

        if (layers != null) {
            for (String l : layers) {
                output.append(Layers.layerNameToDisplayName(l));
                output.append(",");
            }
            number_of_columns += layers.length;
        }

        /* tidy up header */
        output.deleteCharAt(output.length() - 1); //take off end ','
        output.append("\r\n");

        IndexedRecord[] ir = OccurrencesIndex.filterSpeciesRecords(filter);
        int i, j;

        int recordsPos = 0; //for records intersection counter

        ArrayList<String[]> columns = new ArrayList<String[]>();

        try {
            for (IndexedRecord r : ir) {
                columns.clear();

                /*
                 * cap the number of records per read
                 */

                int step = 50000; //max characters to read TODO: move to tabulation settings.xml
                int rstart = r.file_start;
                int rend;

                rend = rstart + step;
                if (rend > r.file_end) {
                    rend = r.file_end;
                }

                String[] sortedrecords;
                int recordstart = r.record_start;
                int recordend;

                String lastpart = "";

                /*
                 * repeat until 20, or so records retrieved
                 */
                int rowoffset = 0;
                results = null;
                while (rowoffset <= max_rows && rend <= r.file_end) {

                    columns.clear();

                    sortedrecords = OccurrencesIndex.getSortedRecords(rstart, rend);

                    sortedrecords[0] = lastpart + sortedrecords[0];

                    columns.add(sortedrecords);

                    if (rend == r.file_end) {
                        //do all records
                        recordend = r.record_end;
                        lastpart = "";
                    } else {
                        //do up to last record
                        lastpart = "";
                        recordend = recordstart + sortedrecords.length - 2; //inclusive
                        lastpart = sortedrecords[sortedrecords.length - 1];
                    }

                    if (layers != null) {
                        for (i = 0; i < layers.length; i++) {
                            columns.add(SamplingIndex.getRecords(
                                    Layers.layerDisplayNameToName(layers[i]),
                                    recordstart,
                                    recordend));
                        }
                    }

                    /* format for csv */
                    int len;
                    if (columns.size() > 1) {
                        len = columns.get(1).length;
                    } else {
                        len = recordend - recordstart + 1;
                    }

                    String[] row;

                    /* output structure */
                    if (results == null) {
                        //limited to max_rows output rows by rowoffset in add loop
                        //+1 for header
                        results = new String[max_rows + 1][number_of_columns + 1];

                        //populate header
                        row = output.toString().split(",");
                        for (j = 0; j < row.length; j++) {
                            results[0][j] = row[j];
                        }
                        rowoffset++;
                    }

                    int coloffset = 0;

                    double[] points = OccurrencesIndex.getPoints(recordstart, recordend);

                    for (j = 0; rowoffset <= max_rows && j < len; j++) {
                        //do not add if does not intersect with records list
                        if (records != null) {
                            int currentRecord = j + recordstart;
                            //increment recordsPos as required
                            while (recordsPos < records.size()
                                    && records.get(recordsPos).intValue() < currentRecord) {
                                recordsPos++;
                            }
                            //test for intersect
                            if (recordsPos >= records.size()
                                    || currentRecord != records.get(recordsPos).intValue()) {
                                continue;
                            }
                        }

                        coloffset = 0;
                        //test bounding box
                        if (region == null || region.isWithin(points[j * 2], points[j * 2 + 1])) {
                            for (i = 0; i < columns.size(); i++) {
                                if (columns.get(i) != null && j < columns.get(i).length) {
                                    if (i == 0) {
                                        row = columns.get(i)[j].split(",");

                                        for (int k = 0; k < row.length; k++) {
                                            results[rowoffset][k] = row[k];
                                        }
                                        coloffset = row.length - 1;
                                    } else if (!(columns.get(i)[j] == null) && !columns.get(i)[j].equals("NaN")) {
                                        results[rowoffset][coloffset] = columns.get(i)[j];
                                    } else {
                                        results[rowoffset][coloffset] = "missing";
                                    }
                                    coloffset++;
                                }
                            }
                            rowoffset++;
                        }
                    }

                    /* adjust for next loop */
                    recordstart = recordend + 1; 		//this was inclusive
                    if (rend < r.file_end) {
                        rstart = rend;				//this is not inclusive
                        rend = rstart + step;
                        if (rend > r.file_end) {
                            rend = r.file_end;
                        }
                    } else {
                        rend = r.file_end + 1;
                    }
                }
            }

            return results;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * gets samples; occurances records + optional intersecting layer values,
     * as csv
     *
     * limit results by a region
     *
     * @param filter species name as String
     * @param layers list of layer names of additional data to include as String []
     * @param region region to restrict results as SimpleRegion
     * @param records sorted pool of records to intersect with as ArrayList<Integer>
     * @return samples as csv, String
     */
    public String sampleSpecies(String filter, String[] layers, SimpleRegion region, ArrayList<Integer> records) {
        StringBuffer output = new StringBuffer();

        OccurrencesFieldsUtil ofu = new OccurrencesFieldsUtil();

        for (String s : ofu.getOutputColumnNames()) {
            output.append(s);
            output.append(",");
        }

        if (layers != null) {
            for (String l : layers) {
                output.append(Layers.layerNameToDisplayName(l));
                output.append(",");
            }
        }

        /* tidy up header */
        output.deleteCharAt(output.length() - 1); //take off end ','
        output.append("\r\n");

        IndexedRecord[] ir = OccurrencesIndex.filterSpeciesRecords(filter);
        int i, j;

        if (ir != null) {
            int column_len = 1;
            if (layers != null) {
                column_len += layers.length;
            }
            ArrayList<String[]> columns = new ArrayList<String[]>(column_len);

            int recordsPos = 0; //position in records list (for intersection test)

            try {
                /* open output file */
                File temporary_file = java.io.File.createTempFile("sample", ".csv");
                FileWriter fw = new FileWriter(temporary_file);

                fw.append(output.toString());


                for (IndexedRecord r : ir) {
                    columns.clear();

                    /*
                     * cap the number of records per read
                     */

                    int step = 5000000; //max characters to read TODO: move to tabulation settings.xmls
                    int rstart = r.file_start;
                    int rend;

                    rend = rstart + step;
                    if (rend > r.file_end) {
                        rend = r.file_end;
                    }

                    String[] sortedrecords;
                    int recordstart = r.record_start;
                    int recordend;
                    String lastpart = "";

                    while (rend <= r.file_end) {

                        columns.clear();

                        sortedrecords = OccurrencesIndex.getSortedRecords(rstart, rend);
                        sortedrecords[0] = lastpart + sortedrecords[0];

                        columns.add(sortedrecords);

                        if (rend == r.file_end) {
                            //do all records
                            recordend = r.record_end;
                            lastpart = "";
                        } else {
                            //do up to last record
                            recordend = recordstart + sortedrecords.length - 2;
                            lastpart = sortedrecords[sortedrecords.length - 1];
                        }

                        for (i = 0; layers != null && i < layers.length; i++) {
                            columns.add(SamplingIndex.getRecords(
                                    Layers.layerDisplayNameToName(layers[i]),
                                    recordstart,
                                    recordend));
                        }

                        double[] points = OccurrencesIndex.getPoints(recordstart, recordend);

                        /* join for output */
                        int len;
                        if (columns.size() < 2) {
                            len = recordend - recordstart + 1;
                        } else {
                            len = columns.get(1).length;
                        }

                        for (j = 0; j < len; j++) {
                            //do not add if does not intersect with records list
                            if (records != null) {
                                int currentRecord = j + recordstart;
                                //increment recordsPos as required
                                while (recordsPos < records.size()
                                        && records.get(recordsPos).intValue() < currentRecord) {
                                    recordsPos++;
                                }
                                //test for intersect
                                if (recordsPos >= records.size()
                                        || currentRecord != records.get(recordsPos).intValue()) {
                                    continue;
                                }
                            }

                            //test bounding box
                            if (region == null || region.isWithin(points[j * 2], points[j * 2 + 1])) {
                                for (i = 0; i < columns.size(); i++) {
                                    if (columns.get(i) != null && j < columns.get(i).length) {
                                        if (!(columns.get(i)[j] == null) && !columns.get(i)[j].equals("NaN")) {
                                            fw.append(columns.get(i)[j]);
                                        }
                                        if (i < columns.size() - 1) {
                                            fw.append(",");
                                        }
                                    }
                                }
                                fw.append("\r\n");
                            }
                        }

                        /* adjust for next loop */
                        recordstart = recordend + 1; 		//this was inclusive
                        if (rend < r.file_end) {
                            rstart = rend;				//this is not inclusive
                            rend = rstart + step;
                            if (rend > r.file_end) {
                                rend = r.file_end;
                            }
                        } else {
                            rend = r.file_end + 1;
                        }
                    }
                }

                fw.close();
                return temporary_file.getPath();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * gets array of points for species (genus, etc) name matches within
     * a specified region
     *
     * @param filter species (genus, etc) name
     * @param region region to filter results by
     * @param records sorted pool of records to intersect with as ArrayList<Integer>
     * @return points as double[], first is longitude, every second is latitude.
     */
    public double[] sampleSpeciesPoints(String filter, SimpleRegion region, ArrayList<Integer> records) {
        IndexedRecord[] ir = OccurrencesIndex.filterSpeciesRecords(filter);

        if (ir != null && ir.length > 0) {

            /* get points */
            double[] points = OccurrencesIndex.getPoints(ir[0].record_start, ir[0].record_end);

            /* test for region absence */
            if (region == null) {
                return points;
            }

            int i;
            int count = 0;

            int recordsPos = 0; //for test on records

            /* return all valid points within the region */
            for (i = 0; i < points.length; i += 2) {
                //do not add if does not intersect with records list
                if (records != null) {
                    int currentRecord = i + ir[0].record_start;
                    //increment recordsPos as required
                    while (recordsPos < records.size()
                            && records.get(recordsPos).intValue() < currentRecord) {
                        recordsPos++;
                    }
                    //test for intersect
                    if (recordsPos >= records.size()
                            || currentRecord != records.get(recordsPos).intValue()) {
                        continue;
                    }
                }
                //region test
                if (region.isWithin(points[i], points[i + 1])) {
                    count += 2;
                } else {
                    points[i] = Double.NaN;
                }
            }
            //move into 'output'
            if (count > 0) {
                double[] output = new double[count];
                int p = 0;
                for (i = 0; i < points.length; i += 2) {
                    if (!Double.isNaN(points[i])) {
                        output[p++] = points[i];
                        output[p++] = points[i + 1];
                    }
                }
                return output;
            }

        }

        return null;
    }
}
