/*
 * Copyright (C) 2013 Vlad Mihalachi
 *
 * This file is part of Turbo Editor.
 *
 * Turbo Editor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Turbo Editor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Turbo Editor. If not, see <http://www.gnu.org/licenses/>.
 */

package com.vmihalachi.turboeditor;

import android.app.ListFragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.vmihalachi.turboeditor.event.FileSelectedEvent;
import com.vmihalachi.turboeditor.event.NewFileOpened;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public class NavigationDrawerListFragment extends ListFragment implements AbsListView.MultiChoiceModeListener {

    List<String> fileNames;
    ArrayAdapter<String> arrayAdapter;

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Out custom layout
        View rootView = inflater.inflate(R.layout.fragment_navigation_drawer, container, false);
        return rootView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setMultiChoiceModeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume() {
        super.onResume();
        // Register the Event Bus for events
        EventBus.getDefault().registerSticky(this);
        //
        refreshList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause() {
        super.onPause();
        // Unregister the Event Bus
        EventBus.getDefault().unregister(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        // don't open the same file twice
        //if(this.mCurrentCheckedPosition == position) return;
        // set current checked position
        //this.mCurrentCheckedPosition = position;
        // Shared Preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        // File paths saved in preferences
        String[] savedPaths = sharedPreferences.getString("savedPaths", "").split(",");
        // Path of the file selected
        String filePath = savedPaths[position];
        // Send the event that a file was selected
        EventBus.getDefault().post(new FileSelectedEvent(filePath));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onItemCheckedStateChanged(ActionMode actionMode, int position, long l, boolean isChecked) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        MenuInflater inflater = actionMode.getMenuInflater();
        inflater.inflate(R.menu.action_mode_navigation_drawer, menu);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.im_remove:
                // Shared Preferences
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                // File paths saved in preferences
                String[] savedPaths = sharedPreferences.getString("savedPaths", "").split(",");
                //
                SparseBooleanArray checkedItems = getListView().getCheckedItemPositions();
                //
                if (checkedItems != null) {
                    for (int i=0; i<checkedItems.size(); i++) {
                        if (checkedItems.valueAt(i)) {
                            removePath(savedPaths[checkedItems.keyAt(i)], false);
                        }
                    }
                    refreshList();
                }
                actionMode.finish();
                return true;
            default:
                return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroyActionMode(ActionMode actionMode) {

    }


    /**
     * When a new file is opened
     * Invoked by the main activity which receive the intent
     * @param event The event called
     */
    public void onEvent(NewFileOpened event){
        EventBus.getDefault().removeStickyEvent(event);
        // Shared Preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        // File paths saved in preferences
        String[] savedPaths = sharedPreferences.getString("savedPaths", "").split(",");

        for(int i = 0; i < savedPaths.length; i++){
            if(savedPaths[i].equals(event.getFilePath())){
                // Send the event that a file was selected
                EventBus.getDefault().post(new FileSelectedEvent(event.getFilePath()));
                return;
            }
        }

        addPath(event.getFilePath());
        // Send the event that a file was selected
        EventBus.getDefault().post(new FileSelectedEvent(event.getFilePath()));
    }

    private void addPath(String path){
        addPath(path, true);
    }

    private void addPath(String path, boolean refresh){
        // Shared Preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        // Editor
        SharedPreferences.Editor editor = sharedPreferences.edit();
        // File paths saved in preferences
        String[] savedPaths = sharedPreferences.getString("savedPaths", "").split(",");
        // StringBuilder
        StringBuilder sb = new StringBuilder();
        // for cycle
        for (int count = 0; count < savedPaths.length; count++) {
            sb.append(savedPaths[count]).append(",");
        }
        // Append new path
        sb.append(path);
        // Put the string
        editor.putString("savedPaths", sb.toString());
        // Commit
        editor.commit();
        // Update list
        //arrayAdapter.add(FilenameUtils.getName(path));
        //arrayAdapter.notifyDataSetChanged();
        if(refresh){
            refreshList();
        }
    }

    private void removePath(String path){
        removePath(path, true);
    }

    private void removePath(String path, boolean refresh){
        // Shared Preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        // Editor
        SharedPreferences.Editor editor = sharedPreferences.edit();
        // File paths saved in preferences
        String[] savedPaths = sharedPreferences.getString("savedPaths", "").split(",");
        // StringBuilder
        StringBuilder sb = new StringBuilder();
        // for cycle
        for (int count = 0; count < savedPaths.length; count++) {
            if(path.equals(savedPaths[count])) continue;
            sb.append(savedPaths[count]).append(",");
        }
        // Put the string
        editor.putString("savedPaths", sb.toString());
        // Commit
        editor.commit();
        // Update list
        //arrayAdapter.remove(FilenameUtils.getName(path));
        //arrayAdapter.notifyDataSetChanged();
        if(refresh){
            refreshList();
        }
    }

    /* package */ void refreshList(){
        // Shared Preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        // File paths saved in preferences
        String[] savedPaths = sharedPreferences.getString("savedPaths", "").split(",");
        // File names for the list
        fileNames = new ArrayList<String>(savedPaths.length);
        //
        StringBuilder sb = new StringBuilder();
        // for cycle to convert paths to names
        for(String path : savedPaths){

            if(!path.isEmpty()){
                File file = new File(path);
                if(file.exists()){
                    fileNames.add(FilenameUtils.getName(path));
                    sb.append(path).append(",");
                }
            }
        }
        // save list without empty or non existed files
        sharedPreferences.edit().putString("savedPaths", sb.toString()).commit();
        // Adapter
        arrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.item_drawer_list, fileNames);
        // Set adapter
        setListAdapter(arrayAdapter);
    }
}
