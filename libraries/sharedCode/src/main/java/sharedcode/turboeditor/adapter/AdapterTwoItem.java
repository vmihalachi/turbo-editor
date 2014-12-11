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

package sharedcode.turboeditor.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import sharedcode.turboeditor.R;

public class AdapterTwoItem extends
        ArrayAdapter<String> {

    private final LayoutInflater inflater;
    private final String[] lines1;
    private final String[] lines2;

    public AdapterTwoItem(Context context,
                          String[] lines1,
                          String[] lines2) {
        super(context, R.layout.item_two_lines, lines1);
        this.lines1 = lines1;
        this.lines2 = lines2;
        this.inflater = LayoutInflater.from(context);
    }


    @Override
    public View getView(final int position,
                        View convertView, final ViewGroup parent) {
        if (convertView == null) {
            convertView = this.inflater
                    .inflate(R.layout.item_two_lines,
                            parent, false);
            final ViewHolder hold = new ViewHolder();
            hold.line1 = (TextView) convertView.findViewById(android.R.id.text1);
            hold.line2 = (TextView) convertView.findViewById(android.R.id.text2);
            convertView.setTag(hold);

            hold.line1.setText(lines1[position]);
            hold.line2.setText(lines2[position]);
        } else {
            final ViewHolder hold = ((ViewHolder) convertView.getTag());
            hold.line1.setText(lines1[position]);
            hold.line2.setText(lines2[position]);
        }
        return convertView;
    }

    public static class ViewHolder {

        public TextView line1;
        public TextView line2;
    }
}
