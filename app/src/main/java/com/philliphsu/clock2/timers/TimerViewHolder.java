package com.philliphsu.clock2.timers;

import android.animation.ObjectAnimator;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.philliphsu.clock2.AsyncTimersTableUpdateHandler;
import com.philliphsu.clock2.BaseViewHolder;
import com.philliphsu.clock2.OnListItemInteractionListener;
import com.philliphsu.clock2.R;
import com.philliphsu.clock2.Timer;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Phillip Hsu on 7/25/2016.
 */
public class TimerViewHolder extends BaseViewHolder<Timer> {
    private static final String TAG = "TimerViewHolder";
    private static final int MAX_PROGRESS = 100000;

    private final AsyncTimersTableUpdateHandler mAsyncTimersTableUpdateHandler;
    private TimerController mController;
    private ObjectAnimator mProgressAnimator;

    @Bind(R.id.label) TextView mLabel;
    @Bind(R.id.duration) CountdownChronometer mChronometer;
    @Bind(R.id.progress_bar) ProgressBar mProgressBar;
    @Bind(R.id.add_one_minute) ImageButton mAddOneMinute;
    @Bind(R.id.start_pause) ImageButton mStartPause;
    @Bind(R.id.stop) ImageButton mStop;

    public TimerViewHolder(ViewGroup parent, OnListItemInteractionListener<Timer> listener,
                           AsyncTimersTableUpdateHandler asyncTimersTableUpdateHandler) {
        super(parent, R.layout.item_timer, listener);
        Log.d(TAG, "New TimerViewHolder");
        mAsyncTimersTableUpdateHandler = asyncTimersTableUpdateHandler;
    }

    @Override
    public void onBind(final Timer timer) {
        super.onBind(timer);
        Log.d(TAG, "Binding TimerViewHolder");
        // TOneverDO: create before super
        mController = new TimerController(timer, mAsyncTimersTableUpdateHandler);
        bindLabel(timer.label());
        bindChronometer(timer);
        bindButtonControls(timer);
        bindProgressBar(timer);
    }

    @OnClick(R.id.start_pause)
    void startPause() {
        mController.startPause();
    }

    @OnClick(R.id.add_one_minute)
    void addOneMinute() {
        mController.addOneMinute();
    }

    @OnClick(R.id.stop)
    void stop() {
        mController.stop();
    }

    private void bindLabel(String label) {
        if (!label.isEmpty()) {
            mLabel.setText(label);
        }
    }

    private void bindChronometer(Timer timer) {
        // In case we're reusing a chronometer instance that could be running:
        // If the Timer instance is not running, this just guarantees the chronometer
        // won't tick, regardless of whether it was running.
        // If the Timer instance is running, we don't care whether the chronometer is
        // also running, because we call start() right after. Stopping it just
        // guarantees that, if it was running, we don't deliver another set of
        // concurrent messages to its handler.
        mChronometer.stop();

        if (!timer.hasStarted()) {
            // Set the initial text
            mChronometer.setDuration(timer.duration());
        } else if (timer.isRunning()) {
            // Re-initialize the base
            mChronometer.setBase(timer.endTime());
            // Previously stopped, so no old messages will interfere.
            mChronometer.start();
        } else {
            // Set the text as last displayed before we stopped.
            // When you call stop() on a Chronometer, it freezes the current text shown,
            // so why do we need this? While that is sufficient for a static View layout,
            // VH recycling will reuse the same Chronometer widget across multiple VHs,
            // so we would have invalid data across those VHs.
            // If a new VH is created, then the chronometer it contains will be in its
            // uninitialized state. We will always need to set the Chronometer's base
            // every time VHs are bound/recycled.
            mChronometer.setDuration(timer.timeRemaining());
        }
    }

    private void bindButtonControls(Timer timer) {
        // TODO: Pause and start icons, resp.
//        mStartPause.setImageResource(timer.isRunning() ? 0 : 0);
        int visibility = timer.hasStarted() ? View.VISIBLE : View.INVISIBLE;
        mAddOneMinute.setVisibility(visibility);
        mStop.setVisibility(visibility);
    }

    private void bindProgressBar(Timer timer) {
        mProgressBar.setMax(MAX_PROGRESS);
        final long timeRemaining = timer.timeRemaining();
        final int progress = (int) (MAX_PROGRESS * (double) timeRemaining / timer.duration());

        // In case we're reusing an animator instance that could be running
        if (mProgressAnimator != null && mProgressAnimator.isRunning()) {
            mProgressAnimator.end();
        }

        if (!timer.isRunning()) {
            mProgressBar.setProgress(progress);
        } else {
            mProgressAnimator = ObjectAnimator.ofInt(
                    // The object that has the property we wish to animate
                    mProgressBar,
                    // The name of the property of the object that identifies which setter method
                    // the animation will call to update its values. Here, a property name of
                    // "progress" will result in a call to the function setProgress() in ProgressBar.
                    // The docs for ObjectAnimator#setPropertyName() says that for best performance,
                    // the setter method should take a float or int parameter, and its return type
                    // should be void (both of which setProgress() satisfies).
                    "progress",
                    // The set of values to animate between. A single value implies that that value
                    // is the one being animated to. Two values imply starting and ending values.
                    // More than two values imply a starting value, values to animate through along
                    // the way, and an ending value (these values will be distributed evenly across
                    // the duration of the animation).
                    progress, 0);
            mProgressAnimator.setDuration(timeRemaining);
            // The algorithm that calculates intermediate values between keyframes. We use linear
            // interpolation so that the animation runs at constant speed.
            mProgressAnimator.setInterpolator(null/*results in linear interpolation*/);
            // This MUST be run on the UI thread.
            mProgressAnimator.start();
        }
    }
}
