/*
 * Copyright (C) 2014 Vlad Mihalachi
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package sharedcode.turboeditor.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import sharedcode.turboeditor.R;
import sharedcode.turboeditor.adapter.AdapterDrawer;
import sharedcode.turboeditor.preferences.PreferenceHelper;
import sharedcode.turboeditor.util.EventBusEvents;


public class NavigationDrawerListFragment extends Fragment implements AdapterView.OnItemClickListener, AdapterDrawer.Callbacks {


    private AdapterDrawer arrayAdapter;
    private ArrayList<File> files;
    private ListView listView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Our custom layout
        View rootView = inflater.inflate(R.layout.fragment_navigation_drawer, container, false);
        listView = (ListView) rootView.findViewById(android.R.id.list);
        listView.setEmptyView(rootView.findViewById(android.R.id.empty));
        files = new ArrayList<>();
        arrayAdapter = new AdapterDrawer(getActivity(), files, this);
        listView.setAdapter(arrayAdapter);
        return rootView;
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listView.setOnItemClickListener(this);
    }


    @Override
    public void onResume() {
        super.onResume();
        // Register the Event Bus for events
        EventBus.getDefault().registerSticky(this);
        // Refresh the list view
        refreshList();
    }


    @Override
    public void onPause() {
        super.onPause();
        // Unregister the Event Bus
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // File paths saved in preferences
        String[] savedPaths = PreferenceHelper.getSavedPaths(getActivity());
        // Path of the file selected
        String filePath = savedPaths[position];
        // Send the event that a file was selected
        EventBus.getDefault().post(new EventBusEvents.NewFileToOpen(new File(filePath)));
        EventBus.getDefault().post(new EventBusEvents.AFileIsSelected(filePath));
    }

    public void onEvent(EventBusEvents.AFileIsSelected event) {
        arrayAdapter.selectView(event.getPath());

        EventBus.getDefault().removeStickyEvent(event);
    }

    public void onEvent(EventBusEvents.NewFileToOpen event) {

        // File paths saved in preferences
        String[] savedPaths = PreferenceHelper.getSavedPaths(getActivity());
        String selectedPath = event.getFile().getAbsolutePath();
        boolean pathAlreadyExist = false;
        for (String savedPath : savedPaths) {
            // We don't need to save the file path twice
            if (savedPath.equals(selectedPath)) {
                pathAlreadyExist = true;
            }
        }
        // Add the path if it wasn't added before
        if(!pathAlreadyExist)
            addPath(selectedPath);

        EventBus.getDefault().removeStickyEvent(event);
    }

    public void onEvent(EventBusEvents.SavedAFile event) {
        if (addPath(event.getPath())) {
            arrayAdapter.selectView(event.getPath());
        }
    }

    public void onEvent(EventBusEvents.ClosedAFile event) {
        arrayAdapter.selectView("");
    }

    private boolean addPath(String path) {
        // File paths saved in preferences
        String[] savedPaths = PreferenceHelper.getSavedPaths(getActivity());
        // StringBuilder
        StringBuilder sb = new StringBuilder();
        boolean pathAlreadyExist = false;
        for (String savedPath : savedPaths) {
            // Append the file path and a comma
            sb.append(savedPath).append(",");
            if (savedPath.equals(path))
                pathAlreadyExist = true;
        }
        // Append new path
        if (!pathAlreadyExist)
            sb.append(path);
        // Put the string and commit
        PreferenceHelper.setSavedPaths(getActivity(), sb);
        // Update list
        refreshList();

        return pathAlreadyExist == false;
    }

    private void removePath(String path) {
        // File paths saved in preferences
        String[] savedPaths = PreferenceHelper.getSavedPaths(getActivity());
        // StringBuilder
        StringBuilder sb = new StringBuilder();
        // for cycle
        for (String savedPath : savedPaths) {
            if (path.equals(savedPath)) continue;
            sb.append(savedPath).append(",");
        }
        // Put the string and commit
        PreferenceHelper.setSavedPaths(getActivity(), sb);
        // Update list
        refreshList();
    }

    private void refreshList() {
        // File paths saved in preferences
        String[] savedPaths = PreferenceHelper.getSavedPaths(getActivity());
        // File names for the list
        files.clear();
        // StringBuilder that will contain the file paths
        StringBuilder sb = new StringBuilder();
        // for cycle to convert paths to names
        for (String path : savedPaths) {
            File file = new File(path);
            // Check that the file exist
            if (file.exists()) {
                files.add(file);
                sb.append(path).append(",");
            }
        }
        // save list without empty or non existed files
        PreferenceHelper.setSavedPaths(getActivity(), sb);
        // Set adapter
        arrayAdapter.notifyDataSetChanged();
    }

    @Override
    public void CancelItem(int position, boolean andCloseOpenedFile) {
        String[] savedPaths = PreferenceHelper.getSavedPaths(getActivity());
        removePath(savedPaths[position]);
        if (andCloseOpenedFile)
            EventBus.getDefault().post(new EventBusEvents.CannotOpenAFile());
    }

    /*@Override
    public void ItemSelected(String path) {
        EventBus.getDefault().post(new EventBusEvents.AFileIsSelected(path));
    }*/
}
