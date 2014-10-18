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

package sharedcode.turboeditor.iab;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.HashSet;

import sharedcode.turboeditor.R;

/**
 * Created by achep on 06.05.14 for AcDisplay.
 *
 * @author Artem Chepurnoy
 */
public class DonationAdapter extends ArrayAdapter<Donation> {

    private final HashSet<String> mInventorySet;
    private final LayoutInflater mInflater;
    private final String mDonationAmountLabel;

    private final int mColorNormal;
    private final int mColorPurchased;

    public DonationAdapter(Context context, Donation[] items, HashSet<String> inventory) {
        super(context, 0, items);

        mInventorySet = inventory;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        Resources res = context.getResources();
        mDonationAmountLabel = res.getString(R.string.donation_item_label);
        mColorNormal = res.getColor(R.color.donation_normal);
        mColorPurchased = res.getColor(R.color.donation_purchased);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Donation donation = getItem(position);
        final Holder holder;
        final View view;

        if (convertView == null) {
            holder = new Holder();
            view = mInflater.inflate(R.layout.donation_iab_item, parent, false);
            assert view != null;

            holder.title = (TextView) view.findViewById(android.R.id.title);
            holder.summary = (TextView) view.findViewById(android.R.id.summary);

            view.setTag(holder);
        } else {
            view = convertView;
            holder = (Holder) view.getTag();
        }

        boolean bought = mInventorySet.contains(donation.sku);

        String amount = Integer.toString(donation.amount);
        holder.title.setText(String.format(mDonationAmountLabel, amount));
        holder.title.setTextColor(bought ? mColorNormal : mColorPurchased);
        holder.summary.setText(donation.text);
        holder.summary.setPaintFlags(bought
                ? holder.summary.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                : holder.summary.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));

        return view;
    }

    private static class Holder {
        TextView title;
        TextView summary;
    }
}
