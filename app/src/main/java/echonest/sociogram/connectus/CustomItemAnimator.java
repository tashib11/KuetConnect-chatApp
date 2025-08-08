package echonest.sociogram.connectus;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

public class CustomItemAnimator extends DefaultItemAnimator {
    private boolean shouldAnimate = true;

    public void setShouldAnimate(boolean shouldAnimate) {
        this.shouldAnimate = shouldAnimate;
    }

    @Override
    public boolean animateAdd(RecyclerView.ViewHolder holder) {
        if (!shouldAnimate) return false; // Skip animation if flag is off
        holder.itemView.setAlpha(0f);
        holder.itemView.setTranslationY(holder.itemView.getHeight());
        holder.itemView.animate()
                .alpha(1f)
                .translationY(0)
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator())
                .withEndAction(() -> dispatchAddFinished(holder))
                .start();
        return true;
    }

    @Override
    public boolean animateRemove(RecyclerView.ViewHolder holder) {
        if (!shouldAnimate) return false; // Skip animation if flag is off
        holder.itemView.animate()
                .alpha(0f)
                .translationY(holder.itemView.getHeight())
                .setDuration(300)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    dispatchRemoveFinished(holder);
                    holder.itemView.setTranslationY(0);
                })
                .start();
        return true;
    }
}
