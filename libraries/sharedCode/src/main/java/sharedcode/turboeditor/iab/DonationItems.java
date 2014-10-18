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

import android.content.res.Resources;

import sharedcode.turboeditor.R;

/**
 * Created by achep on 07.05.14 for AcDisplay.
 *
 * @author Artem Chepurnoy
 */
public class DonationItems {

    public static Donation[] get(Resources res) {
        int[] data = new int[]{
                2, R.string.donation_2,
                4, R.string.donation_4,
                10, R.string.donation_10,
                20, R.string.donation_20,
                50, R.string.donation_50,
                99, R.string.donation_99,
        };

        Donation[] donation = new Donation[data.length / 2];

        int length = donation.length;
        for (int i = 0; i < length; i++) {
            donation[i] = new Donation(data[i * 2],
                    res.getString(data[i * 2 + 1]));
        }
        return donation;
    }

}
