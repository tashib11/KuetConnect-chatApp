package echonest.sociogram.connectus.Adapters;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

import echonest.sociogram.connectus.Models.ModelChat;

public class ChatDiffCallback extends DiffUtil.Callback {
    private final List<ModelChat> oldList;
    private final List<ModelChat> newList;

    public ChatDiffCallback(List<ModelChat> oldList, List<ModelChat> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).getTimestamp().equals(newList.get(newItemPosition).getTimestamp());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
    }
}

