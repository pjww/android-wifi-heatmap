package jaxon.bdpp.utils;


import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import jaxon.bdpp.logic.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class Logger {
    public static void DumpSignalGrids(MainData mainData) {
        if (mainData.getGridInfo() == null)
            return;

        String filename = Environment.getExternalStorageDirectory().toString() + "/signal_grids.txt";
        FileOutputStream outputStream;

        try {
            outputStream = new FileOutputStream(filename);
            outputStream.write(GetSignalGridsDump(mainData).getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String GetSignalGridsDump(MainData mainData) throws JSONException {
        String output = "";
        JSONObject json = new JSONObject();
        JSONObject json1 = new JSONObject();
        JSONArray jsonArray = new JSONArray();


        for (WifiNetwork wifiNetwork : mainData.getSignalGrids().keySet()) {
            json.put("wifi", wifiNetwork.toString());
            json1.put(String.valueOf(json), jsonArray);

            SignalGrid signalGrid = mainData.getSignalGrids().get(wifiNetwork);

            for (int y = 0 ; y < signalGrid.getGridInfo().getRowsCount() ; ++y) {
                //output += y; //"Row " + y + ": ";
                int x;
                for (x = 0; x < signalGrid.getGridInfo().getColumnsCount(); ++x) {
                    output += signalGrid.getSignalInfo(new CellPosition(y, x)).getAverageSignalLevel();
                    jsonArray.put(signalGrid.getSignalInfo(new CellPosition(y, x)).getAverageSignalLevel());
                }
                output += "\n";
            }
            output += "\n";
        }


        try (FileWriter file = new FileWriter(Environment.getExternalStorageDirectory().toString() + "/signal_grids.json")) {
            file.write(json1.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return output;
    }
}
