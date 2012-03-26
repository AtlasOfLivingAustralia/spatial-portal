package org.ala.spatial.analysis.maxent;

import java.util.Iterator;
import java.util.List;
import org.ala.spatial.util.AlaspatialProperties;
import org.ala.spatial.util.Layers;

/**
 * Settings for the MaxEnt process. Sets up the basic/default settings
 * allow for more settings to be added as needed
 * 
 * @author ajayr
 */
public class MaxentSettings {

    private String maxentPath;
    private boolean doJackknife;
    private boolean doResponsecurves;
    private int randomTestPercentage;
    private String speciesFilepath;
    private String envVarToggler;
    private String envPath;
    private String envPrefix;
    private String envSuffix;
    private List envList;
    private String outputPath;
    private String defaultCmdVars;

    public MaxentSettings() {
        //maxentPath = "C:/projects/biomaps/modelling/maxent3/runmaxent.bat ";
        maxentPath = "java -mx900m -jar /Users/ajay/projects/modelling/maxent/maxent.jar ";

        envVarToggler = "";
        envPrefix = "";
        envSuffix = "";

        doJackknife = false;
        doResponsecurves = false;
        randomTestPercentage = 0;

        defaultCmdVars = " -a warnings=false tooltips=false -z ";// -outputfiletype asc ";
    }

    public String getDefaultCmdVars() {
        return defaultCmdVars;
    }

    public void setDefaultCmdVars(String defaultCmdVars) {
        this.defaultCmdVars = defaultCmdVars;
    }

    public boolean isDoJackknife() {
        return doJackknife;
    }

    public void setDoJackknife(boolean doJackknife) {
        this.doJackknife = doJackknife;
    }

    public boolean isDoResponsecurves() {
        return doResponsecurves;
    }

    public void setDoResponsecurves(boolean doResponsecurves) {
        this.doResponsecurves = doResponsecurves;
    }

    public String getEnvPath() {
        return envPath;
    }

    public void setEnvPath(String envPath) {
        this.envPath = envPath;
    }

    public String getEnvPrefix() {
        return envPrefix;
    }

    public void setEnvPrefix(String envPrefix) {
        this.envPrefix = envPrefix;
    }

    public String getEnvSuffix() {
        return envSuffix;
    }

    public void setEnvSuffix(String envSuffix) {
        this.envSuffix = envSuffix;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public int getRandomTestPercentage() {
        return randomTestPercentage;
    }

    public void setRandomTestPercentage(int randomTestPercentage) {
        this.randomTestPercentage = randomTestPercentage;
    }

    public String getSpeciesFilepath() {
        return speciesFilepath;
    }

    public void setSpeciesFilepath(String speciesFilepath) {
        this.speciesFilepath = speciesFilepath;
    }

    public String getEnvVarToggler() {
        return envVarToggler;
    }

    public void setEnvVarToggler(String envVarToggler) {
        this.envVarToggler = envVarToggler;
    }

    public String getMaxentPath() {
        return maxentPath;
    }

    public void setMaxentPath(String maxentPath) {
        this.maxentPath = maxentPath;
    }

    public List<String> getEnvList() {
        return envList;
    }

    public void setEnvList(List<String> envList) {
        for(int i=0;i<envList.size();i++) {
            envList.set(i, Layers.getFieldId(envList.get(i)));
        }
        this.envList = envList;
    }

    @Override
    public String toString() {
        String cmd;

        cmd = "";

        // add the maxent path
        cmd += maxentPath;

        // add the env.vars path
        cmd += " -e " + getEnvPath();

        // add the sp filepath
        cmd += " -s " + getSpeciesFilepath();

        // add the default cmd vars
        cmd += defaultCmdVars;

        // set thread count
        cmd += " threads=" + AlaspatialProperties.getAnalysisThreadCount();

        // add the random test percentage
        cmd += " -X " + randomTestPercentage;

        // add jackknife if selected
        if (doJackknife) {
            cmd += " -J ";
        }

        // add response curves
        if (doResponsecurves) {
            cmd += " -P ";
        }

        // add future variables if available
        //cmd += " -j " + futureEnvPath;

        // add the env.var toggler
        //cmd += " -N " + envVarToggler;
        if (envVarToggler.length() > 0) {
            String[] ctxlist = envVarToggler.split(" ");
            for (String ctx : ctxlist) {
                cmd += " -t " + ctx;
            }
        }

        // add the env vars
        for (Iterator<String> itr = envList.iterator(); itr.hasNext();) {
            String eval = itr.next();

            if (eval.length() == 1) {
                eval = "0" + eval;
            }

            //  cmd += " -N " + envPrefix + eval + envSuffix;

        }

        // finally add the output path
        cmd += " -o " + outputPath;

        return cmd;
    }
}
