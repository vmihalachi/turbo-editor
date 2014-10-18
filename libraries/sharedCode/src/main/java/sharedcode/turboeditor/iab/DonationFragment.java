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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.HashSet;

import sharedcode.turboeditor.R;
import sharedcode.turboeditor.iab.utils.IabHelper;
import sharedcode.turboeditor.iab.utils.IabResult;
import sharedcode.turboeditor.iab.utils.Inventory;
import sharedcode.turboeditor.iab.utils.Purchase;
import sharedcode.turboeditor.preferences.PreferenceHelper;
import sharedcode.turboeditor.util.Build;
import sharedcode.turboeditor.util.ToastUtils;
import sharedcode.turboeditor.util.ViewUtils;
import sharedcode.turboeditor.views.DialogHelper;

/**
 * Fragment that represents an ability to donate to me. Be sure to redirect
 * {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
 * to this fragment!
 *
 * @author Artem Chepurnoy
 */
public class DonationFragment extends DialogFragment {

    public static final int RC_REQUEST = 10001;
    private static final String TAG = "DonationFragment";
    private final HashSet<String> mInventorySet = new HashSet<>();
    private GridView mGridView;
    private ProgressBar mProgressBar;
    private TextView mError;
    private IabHelper mHelper;
    private final IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener =
            new IabHelper.OnIabPurchaseFinishedListener() {
                public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
                    if (mHelper == null) return;
                    if (result.isFailure()) {
                        complain("Error purchasing: " + result);
                        setWaitScreen(false);
                        return;
                    }

                    if (!verifyDeveloperPayload(purchase)) {
                        complain("Error purchasing. Authenticity verification failed.");
                        setWaitScreen(false);
                        return;
                    }

                    // else, it is a success, the user has donated!
                    String sku = purchase.getSku();
                    mInventorySet.add(sku);
                    PreferenceHelper.setHasDonated(getActivity(), true);
                }
            };
    private Donation[] mDonationList;
    private final IabHelper.QueryInventoryFinishedListener mGotInventoryListener =
            new IabHelper.QueryInventoryFinishedListener() {
                public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
                    if (mHelper == null) return;
                    if (result.isFailure()) {
                        complain("Failed to query inventory: " + result);
                        return;
                    }

                    mInventorySet.clear();
                    for (Donation donation : mDonationList) {
                        Purchase purchase = inventory.getPurchase(donation.sku);
                        boolean isBought = (purchase != null && verifyDeveloperPayload(purchase));

                        if (isBought) {
                            mInventorySet.add(donation.sku);
                            PreferenceHelper.setHasDonated(getActivity(), true);
                        }
                    }

                    /*
                    // Fake items to debug user interface.
                    mInventorySet.add(mDonationList[0].sku);
                    mInventorySet.add(mDonationList[1].sku);
                    mInventorySet.add(mDonationList[2].sku);
                    */

                    updateUi();
                    setWaitScreen(false);
                }
            };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mDonationList = DonationItems.get(getResources());
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        assert activity != null;

        View view = new DialogHelper.Builder(activity)
                .setTitle(R.string.donation_title)
                .setView(R.layout.donation_dialog)
                .createSkeletonView();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setView(view)
                .setNegativeButton(android.R.string.cancel, null);

        TextView info = (TextView) view.findViewById(R.id.info);
        info.setText(Html.fromHtml(getString(R.string.donation_info)));
        info.setMovementMethod(new LinkMovementMethod());

        mError = (TextView) view.findViewById(R.id.error);
        mProgressBar = (ProgressBar) view.findViewById(android.R.id.progress);
        mGridView = (GridView) view.findViewById(R.id.grid);
        mGridView.setAdapter(new DonationAdapter(getActivity(), mDonationList, mInventorySet));
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DonationAdapter adapter = (DonationAdapter) parent.getAdapter();
                Donation donation = adapter.getItem(position);

                if (!mInventorySet.contains(donation.sku)) {
                    /**
                     * See {@link sharedcode.turboeditor.iab.DonationFragment#verifyDeveloperPayload(Purchase)}.
                     */
                    String payload = "";
                    try {
                        mHelper.launchPurchaseFlow(
                                getActivity(), donation.sku, RC_REQUEST,
                                mPurchaseFinishedListener, payload);
                    } catch (Exception e) {
                        ToastUtils.showShort(getActivity(), "Failed to launch a purchase flow.");
                    }
                } else {
                    ToastUtils.showShort(getActivity(), getString(R.string.donation_item_bought));
                }
            }
        });

        final AlertDialog alertDialog;

        // Show PayPal button.
        final Intent paypalIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Build.Links.DONATE));
        builder.setNeutralButton(R.string.paypal, null);

        alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {
                Data[] datas = new Data[]{
                        new Data(
                                alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL),
                                paypalIntent, R.drawable.ic_action_paypal)
                };

                ImageSpan span;
                SpannableString text;
                for (final Data data : datas) {
                    final Button btn = data.button;
                    if (btn != null) {
                        span = new ImageSpan(getActivity(), data.iconResource);

                        // Replace text with an icon.
                        // This is a workaround to fix compound button's aligment.
                        text = new SpannableString(" ");
                        text.setSpan(span, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        btn.setText(text);

                        // Eat default weight.
                        btn.setLayoutParams(new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT));

                        btn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                startPaymentIntentWithWarningAlertDialog(data.intent);
                            }
                        });
                    }
                }
            }

            final class Data {

                private final Button button;
                private final Intent intent;
                private final int iconResource;

                private Data(Button button, Intent intent, int iconResource) {
                    this.button = button;
                    this.intent = intent;
                    this.iconResource = iconResource;
                }
            }
        });

        initBilling();

        return alertDialog;
    }

    /**
     * Shows a warning alert dialog to note, that those methods
     * may suck hard and nobody will care about it.<br/>
     * Starts an intent if user is agree with it.
     */
    private void startPaymentIntentWithWarningAlertDialog(final Intent intent) {
        CharSequence messageText = getString(R.string.donation_no_responsibility);
        new DialogHelper.Builder(getActivity())
                .setMessage(messageText)
                .wrap()
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            startActivity(intent);
                            dismiss(); // Dismiss main fragment
                        } catch (ActivityNotFoundException e) { /* hell no */ }
                    }
                })
                .create()
                .show();
    }

    private void setWaitScreen(boolean loading) {
        ViewUtils.setVisible(mProgressBar, loading);
        ViewUtils.setVisible(mGridView, !loading);
        ViewUtils.setVisible(mError, false);
    }

    private void setErrorScreen(String errorMessage, final Runnable runnable) {
        mProgressBar.setVisibility(View.GONE);
        mGridView.setVisibility(View.GONE);
        mError.setVisibility(View.VISIBLE);
        mError.setText(errorMessage);
        mError.setOnClickListener(runnable != null ? new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runnable.run();
            }
        } : null);
    }

    /**
     * Updates GUI to display changes.
     */
    private void updateUi() {
        DonationAdapter adapter = (DonationAdapter) mGridView.getAdapter();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposeBilling();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mHelper.handleActivityResult(requestCode, resultCode, data)) {
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Releases billing service.
     *
     * @see #initBilling()
     */
    private void disposeBilling() {
        if (mHelper != null) {
            mHelper.dispose();
            mHelper = null;
        }
    }

    /**
     * <b>Make sure you call {@link #disposeBilling()}!</b>
     *
     * @see #disposeBilling()
     */
    private void initBilling() {
        setWaitScreen(true);
        disposeBilling();

        String base64EncodedPublicKey = Build.GOOGLE_PLAY_PUBLIC_KEY;
        mHelper = new IabHelper(getActivity(), base64EncodedPublicKey);
        mHelper.enableDebugLogging(Build.DEBUG);
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (mHelper == null) return;
                if (!result.isSuccess()) {
                    setErrorScreen(getString(R.string.donation_error_iab_setup), new Runnable() {
                        @Override
                        public void run() {
                            // Try to initialize billings again.
                            initBilling();
                        }
                    });
                    return;
                }

                setWaitScreen(false);
                mHelper.queryInventoryAsync(mGotInventoryListener);
            }
        });
    }

    private boolean verifyDeveloperPayload(Purchase purchase) {
        // TODO: This method itself is a big question.
        // Personally, I think that this whole ‘best practices’ part
        // is confusing and is trying to make you do work that the API
        // should really be doing. Since the purchase is tied to a Google account,
        // and the Play Store obviously saves this information, they should
        // just give you this in the purchase details. Getting a proper user ID
        // requires additional permissions that you shouldn’t need to add just
        // to cover for the deficiencies of the IAB API.
        return true;
    }

    private void complain(String message) {
        ToastUtils.showShort(getActivity(), message);
    }

}
