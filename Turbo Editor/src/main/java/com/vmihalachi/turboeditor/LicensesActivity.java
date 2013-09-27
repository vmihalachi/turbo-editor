package com.vmihalachi.turboeditor;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class LicensesActivity extends Activity implements AdapterView.OnItemClickListener{

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_licenses);
        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.open_source_libs));
        listView.setAdapter(adapter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String openSourceLib = ((TextView) view.findViewById(android.R.id.text1)).getText().toString();
        Intent browserIntent = null;
        if(openSourceLib.equals("ChangeLog Library")){
            browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/gabrielemariotti/changeloglib?source=c#license"));
        } else if(openSourceLib.equals("EventBus")){
            browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/greenrobot/EventBus?source=c#license"));
        } else if(openSourceLib.equals("commons-io")){
            browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://commons.apache.org/proper/commons-io/"));
        }
        if(browserIntent != null){
            startActivity(browserIntent);
        }
    }
}
