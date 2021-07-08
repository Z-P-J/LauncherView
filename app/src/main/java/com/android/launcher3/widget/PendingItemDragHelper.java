/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.widget;

import android.graphics.Point;
import android.graphics.Rect;
import android.view.View;

import com.android.launcher3.DragSource;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherActivity;
import com.android.launcher3.TabItemInfo;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.graphics.DragPreviewProvider;
import com.qianxun.browser.database.HomepageManager;
import com.qianxun.browser.model.FavoriteItem;

/**
 * Extension of {@link DragPreviewProvider} with logic specific to pending widgets/shortcuts
 * dragged from the widget tray.
 */
public class PendingItemDragHelper extends DragPreviewProvider {

    private static final float MAX_WIDGET_SCALE = 1.25f;

    private final ItemInfo mAddInfo;
    private int[] mEstimatedCellSize;

    public PendingItemDragHelper(View view) {
        super(view);
//        mAddInfo = (ItemInfo) view.getTag();
        FavoriteItem item = HomepageManager.getInstance().getAllFavorites().get(0);
        mAddInfo = new TabItemInfo();
        item.applyCommonProperties(mAddInfo);
        mAddInfo.itemType = ItemInfo.ITEM_TYPE_WIDGET;
        mAddInfo.spanX = 2;
        mAddInfo.spanY = 2;
        mAddInfo.title = "TabCard";
        mAddInfo.id = ItemInfo.NO_ID;
    }

    public void startDrag(Rect previewBounds, int previewBitmapWidth, int previewViewWidth,
                          Point screenPos, DragSource source, DragOptions options) {
        final LauncherActivity launcher = LauncherActivity.fromContext(mView.getContext());
//        LauncherAppState app = LauncherAppState.getInstance(mView.getContext());

        final float scale;
        final Point dragOffset;
        final Rect dragRegion;

        mEstimatedCellSize = launcher.getLauncherLayout().getWorkspace().estimateItemSize(mAddInfo);

//        int maxWidth = Math.min((int) (previewBitmapWidth * MAX_WIDGET_SCALE), mEstimatedCellSize[0]);

        int[] previewSizeBeforeScale = new int[1];

        if (previewSizeBeforeScale[0] < previewBitmapWidth) {
            // The icon has extra padding around it.
            int padding = (previewBitmapWidth - previewSizeBeforeScale[0]) / 2;
            if (previewBitmapWidth > previewViewWidth) {
                padding = padding * previewViewWidth / previewBitmapWidth;
            }

            previewBounds.left += padding;
            previewBounds.right -= padding;
        }
        scale = previewBounds.width() / (float) previewViewWidth;
//        launcher.getLauncherLayout().getDragController().addDragListener(new WidgetHostViewLoader(launcher, mView));

        dragOffset = null;
        dragRegion = null;

        // Since we are not going through the workspace for starting the drag, set drag related
        // information on the workspace before starting the drag.
        launcher.getLauncherLayout().getWorkspace().prepareDragWithProvider(this);

        int dragLayerX = screenPos.x + previewBounds.left
                + (int) ((scale * previewViewWidth - previewViewWidth) / 2);
        int dragLayerY = screenPos.y + previewBounds.top
                + (int) ((scale * previewViewWidth - previewViewWidth) / 2);

        // Start the drag
        launcher.getLauncherLayout().getDragController().startDrag(createDragBitmap(), dragLayerX, dragLayerY, source, mAddInfo,
                dragOffset, dragRegion, scale, scale, options);
    }

//    @Override
//    protected Bitmap convertPreviewToAlphaBitmap(Bitmap preview) {
//        if (mEstimatedCellSize == null) {
//            return super.convertPreviewToAlphaBitmap(preview);
//        }
//
//        int w = mEstimatedCellSize[0];
//        int h = mEstimatedCellSize[1];
//        final Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8);
//        Rect src = new Rect(0, 0, preview.getWidth(), preview.getHeight());
//
//        float scaleFactor = Math.min((w - blurSizeOutline) / (float) preview.getWidth(),
//                (h - blurSizeOutline) / (float) preview.getHeight());
//        int scaledWidth = (int) (scaleFactor * preview.getWidth());
//        int scaledHeight = (int) (scaleFactor * preview.getHeight());
//        Rect dst = new Rect(0, 0, scaledWidth, scaledHeight);
//
//        // center the image
//        dst.offset((w - scaledWidth) / 2, (h - scaledHeight) / 2);
//        new Canvas(b).drawBitmap(preview, src, dst, new Paint(Paint.FILTER_BITMAP_FLAG));
//        return b;
//    }
}
