package resakemal.catpoints.Controller;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;

import resakemal.catpoints.R;

/**
 * Created by Resa Kemal Saharso on 05/12/2017.
 */

public class FactManager {
    ArrayList<String> factList;

    public FactManager() {
        factList = new ArrayList<>();
    }
    public ArrayList<String> readFactFile(InputStream input, TextView fact_bar) {
        String fileName = "cat_facts.txt";

        String line = null;
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(input);
            BufferedReader bufferedReader =
                    new BufferedReader(inputStreamReader);
            while((line = bufferedReader.readLine()) != null) {
                factList.add(line);
            }
            bufferedReader.close();
        }
        catch(FileNotFoundException ex) {
            System.out.println(
                    "Unable to open file '" +
                            fileName + "'");
        }
        catch(IOException ex) {
            System.out.println(
                    "Error reading file '"
                            + fileName + "'");
            // Or we could just do this:
            // ex.printStackTrace();
        }
        Log.d("factList",factList.toString());
        return factList;
    }
}
