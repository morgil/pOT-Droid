package com.mde.potdroid.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import com.mde.potdroid.R;
import com.mde.potdroid.views.IconButton;

/**
 * This Fragment extends BaseFragment and provides some more methods and an interface
 * for those Fragments who have pagination functionality.
 */
abstract public class PaginateFragment extends BaseFragment {

    private LinearLayout mPaginateLayout;

    public abstract void goToFirstPage();

    public abstract void goToLastPage();

    public abstract void goToNextPage();

    public abstract void goToPrevPage();

    public abstract boolean isFirstPage();

    public abstract boolean isLastPage();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        refreshPaginateLayout();
    }

    public void refreshPaginateLayout() {
        mPaginateLayout = getBaseActivity().getPaginateLayout();

        //IconButton refreshButton = (IconButton) paginateWidget.findViewById(R.id.button_refresh);
        IconButton fwdButton = (IconButton) mPaginateLayout.findViewById(R.id.button_fwd);
        IconButton ffwdButton = (IconButton) mPaginateLayout.findViewById(R.id.button_ffwd);
        IconButton rwdButton = (IconButton) mPaginateLayout.findViewById(R.id.button_rwd);
        IconButton frwdButton = (IconButton) mPaginateLayout.findViewById(R.id.button_frwd);

        fwdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToNextPage();
            }
        });

        ffwdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToLastPage();
            }
        });

        rwdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToPrevPage();
            }
        });

        frwdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToFirstPage();
            }
        });

        boolean anyVisible = false;

        // only show the paginate buttons if there are before or after the current
        if (isLastPage()) {
            fwdButton.setVisibility(View.INVISIBLE);
            ffwdButton.setVisibility(View.INVISIBLE);
        } else {
            anyVisible = true;
            fwdButton.setVisibility(View.VISIBLE);
            ffwdButton.setVisibility(View.VISIBLE);
        }

        if (isFirstPage()) {
            rwdButton.setVisibility(View.INVISIBLE);
            frwdButton.setVisibility(View.INVISIBLE);
        } else {
            anyVisible = true;
            rwdButton.setVisibility(View.VISIBLE);
            frwdButton.setVisibility(View.VISIBLE);
        }

        if(anyVisible)
            getBaseActivity().showPaginateView();
        else
            getBaseActivity().hidePaginateView();

    }


}
