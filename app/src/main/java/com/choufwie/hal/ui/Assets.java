package com.choufwie.hal.ui;

import android.content.Context;

import com.choufwie.hal.R;

/**
 * Finds YOUR images by file name (no code change needed):
 *   res/drawable/jannet.png            -> the fortune-teller popup + login screen
 *   res/drawable/meme_1.png, meme_2.png, ... (as many as you want, numbered without gaps) -> home memes
 * If an image is missing, a placeholder is shown instead of crashing.
 */
public final class Assets {
    private Assets() {}

    private static int res(Context c, String name) {
        int id = c.getResources().getIdentifier(name, "drawable", c.getPackageName());
        return id != 0 ? id : R.drawable.ic_placeholder;
    }

    public static int jannet(Context c) { return res(c, "jannet"); }

    public static int memeCount(Context c) {
        int n = 0;
        while (c.getResources().getIdentifier("meme_" + (n + 1), "drawable", c.getPackageName()) != 0) n++;
        return n;
    }

    public static int meme(Context c, int index) { return res(c, "meme_" + (index + 1)); }
}
