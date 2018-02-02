package resakemal.catpoints;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Context context = this;

        TextView desc = findViewById(R.id.about_desc);
        desc.setText(readFactFile(context));
    }

    public String readFactFile(Context context) {
        String fileName = "about.txt";
        String about_text = "";
        InputStream is = context.getResources().openRawResource(R.raw.about);
        String line = null;
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(is);
            BufferedReader bufferedReader =
                    new BufferedReader(inputStreamReader);
            while((line = bufferedReader.readLine()) != null) {
                about_text += line + "\n";
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
        Log.d("factList",about_text);
        return about_text;
    }
}
