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

package com.vmihalachi.turboeditor.fragment;

import android.app.ListFragment;
import android.os.Bundle;
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

import com.vmihalachi.turboeditor.R;
import com.vmihalachi.turboeditor.event.FileSelectedEvent;
import com.vmihalachi.turboeditor.event.NewFileOpened;
import com.vmihalachi.turboeditor.helper.PreferenceHelper;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public class NavigationDrawerListFragment extends ListFragment implements AbsListView.MultiChoiceModeListener {

    private List<String> fileNames;
    private ArrayAdapter<String> arrayAdapter;

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Our custom layout
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
        // Refresh the list view
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
        // File paths saved in preferences
        String[] savedPaths = PreferenceHelper.getSavedPaths(getActivity());
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
                // File paths saved in preferences
                String[] savedPaths = PreferenceHelper.getSavedPaths(getActivity());
                // We get the checked positions
                SparseBooleanArray checkedItems = getListView().getCheckedItemPositions();
                // If we have some checked positions
                if (checkedItems != null) {
                    for (int i=0; i<checkedItems.size(); i++) {
                        // check if the value is checked
                        if (checkedItems.valueAt(i)) {
                            // remove the checked path, but don't refresh the list
                            removePath(savedPaths[checkedItems.keyAt(i)], false);
                        }
                    }
                    // In the end refresh the list
                    refreshList();
                }
                // Close the action mode
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
        // File paths saved in preferences
        String[] savedPaths = PreferenceHelper.getSavedPaths(getActivity());
        for(int i = 0; i < savedPaths.length; i++){
            // We don't need to save the file path twice
            if(savedPaths[i].equals(event.getFilePath())){
                // Send the event that a file was selected
                EventBus.getDefault().post(new FileSelectedEvent(event.getFilePath()));
                return;
            }
        }
        // Add the path if it wasn't added before
        addPath(event.getFilePath());
        // Send the event that a file was selected
        EventBus.getDefault().post(new FileSelectedEvent(event.getFilePath()));
    }

    private void addPath(String path){
        // Add a path and refresh the list
        addPath(path, true);
    }

    private void addPath(String path, boolean refreshTheList){
        // File paths saved in preferences
        String[] savedPaths = PreferenceHelper.getSavedPaths(getActivity());
        // StringBuilder
        StringBuilder sb = new StringBuilder();
        for (int count = 0; count < savedPaths.length; count++) {
            // Append the file path and a comma
            sb.append(savedPaths[count]).append(",");
        }
        // Append new path
        sb.append(path);
        // Put the string and commit
        PreferenceHelper.setSavedPaths(getActivity(), sb);
        // Update list
        if(refreshTheList){
            refreshList();
        }
    }

    private void removePath(String path){
        // Remove the path and refresh the list
        removePath(path, true);
    }

    private void removePath(String path, boolean refresh){
        // File paths saved in preferences
        String[] savedPaths = PreferenceHelper.getSavedPaths(getActivity());
        // StringBuilder
        StringBuilder sb = new StringBuilder();
        // for cycle
        for (int count = 0; count < savedPaths.length; count++) {
            if(path.equals(savedPaths[count])) continue;
            sb.append(savedPaths[count]).append(",");
        }
        // Put the string and commit
        PreferenceHelper.setSavedPaths(getActivity(), sb);
        // Update list
        if(refresh){
            refreshList();
        }
    }

    private  void refreshList(){
        // File paths saved in preferences
        String[] savedPaths = PreferenceHelper.getSavedPaths(getActivity());
        // File names for the list
        fileNames = new ArrayList<String>(savedPaths.length);
        // StringBuilder that will contain the file paths
        StringBuilder sb = new StringBuilder();
        // for cycle to convert paths to names
        for(String path : savedPaths){
            File file = new File(path);
            // Check that the file exist
            if(file.exists()){
                fileNames.add(FilenameUtils.getName(path));
                sb.append(path).append(",");
            }
        }
        // save list without empty or non existed files
        PreferenceHelper.setSavedPaths(getActivity(), sb);
        // Adapter
        arrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.item_drawer_list, fileNames);
        // Set adapter
        setListAdapter(arrayAdapter);
    }
}
