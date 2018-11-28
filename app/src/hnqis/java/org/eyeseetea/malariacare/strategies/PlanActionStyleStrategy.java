package org.eyeseetea.malariacare.strategies;

import android.support.design.widget.FloatingActionButton;
import android.widget.RelativeLayout;

import org.eyeseetea.malariacare.R;
import org.eyeseetea.malariacare.domain.entity.ObservationStatus;
import org.eyeseetea.sdk.presentation.views.DoubleRectChart;

public class PlanActionStyleStrategy {
    public static void fabIcons(FloatingActionButton fabComplete, ObservationStatus status) {
        if (status.equals(ObservationStatus.IN_PROGRESS)) {
            fabComplete.setImageResource(R.drawable.ic_action_uncheck);
        } else if (status.equals(ObservationStatus.SENT)) {
            fabComplete.setImageResource(R.drawable.ic_double_check);
        } else {
            fabComplete.setImageResource(R.drawable.ic_action_check);
        }
    }

    public static DoubleRectChart loadDoubleRectChart(RelativeLayout rootView) {
        return null;
    }

    public static void disableShare(FloatingActionButton mFabComplete) {
    }
    public static void enableShare(FloatingActionButton mFabComplete) {
    }
}