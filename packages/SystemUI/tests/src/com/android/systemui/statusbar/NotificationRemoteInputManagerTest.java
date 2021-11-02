
package com.android.systemui.statusbar;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Notification;
import android.app.RemoteInputHistoryItem;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.UserHandle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.testing.AndroidTestingRunner;
import android.testing.TestableLooper;

import androidx.annotation.NonNull;
import androidx.test.filters.SmallTest;

import com.android.systemui.SysuiTestCase;
import com.android.systemui.dump.DumpManager;
import com.android.systemui.flags.FeatureFlags;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.NotificationRemoteInputManager.LegacyRemoteInputLifetimeExtender.RemoteInputActiveExtender;
import com.android.systemui.statusbar.NotificationRemoteInputManager.LegacyRemoteInputLifetimeExtender.RemoteInputHistoryExtender;
import com.android.systemui.statusbar.NotificationRemoteInputManager.LegacyRemoteInputLifetimeExtender.SmartReplyHistoryExtender;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.collection.NotificationEntryBuilder;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.policy.RemoteInputUriController;

import com.google.android.collect.Sets;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import dagger.Lazy;

@SmallTest
@RunWith(AndroidTestingRunner.class)
@TestableLooper.RunWithLooper
public class NotificationRemoteInputManagerTest extends SysuiTestCase {
    private static final String TEST_PACKAGE_NAME = "test";
    private static final int TEST_UID = 0;

    @Mock private NotificationPresenter mPresenter;
    @Mock private RemoteInputController.Delegate mDelegate;
    @Mock private NotificationRemoteInputManager.Callback mCallback;
    @Mock private RemoteInputController mController;
    @Mock private SmartReplyController mSmartReplyController;
    @Mock private NotificationListenerService.RankingMap mRanking;
    @Mock private ExpandableNotificationRow mRow;
    @Mock private StatusBarStateController mStateController;
    @Mock private RemoteInputUriController mRemoteInputUriController;
    @Mock private NotificationClickNotifier mClickNotifier;

    // Dependency mocks:
    @Mock private NotificationEntryManager mEntryManager;
    @Mock private NotificationLockscreenUserManager mLockscreenUserManager;

    private TestableNotificationRemoteInputManager mRemoteInputManager;
    private NotificationEntry mEntry;
    private RemoteInputHistoryExtender mRemoteInputHistoryExtender;
    private SmartReplyHistoryExtender mSmartReplyHistoryExtender;
    private RemoteInputActiveExtender mRemoteInputActiveExtender;
    private TestableNotificationRemoteInputManager.FakeLegacyRemoteInputLifetimeExtender
            mLegacyRemoteInputLifetimeExtender;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mRemoteInputManager = new TestableNotificationRemoteInputManager(mContext,
                mock(FeatureFlags.class),
                mLockscreenUserManager, mSmartReplyController, mEntryManager,
                () -> Optional.of(mock(StatusBar.class)),
                mStateController,
                Handler.createAsync(Looper.myLooper()),
                mRemoteInputUriController,
                mClickNotifier,
                mock(ActionClickLogger.class),
                mock(DumpManager.class));
        mEntry = new NotificationEntryBuilder()
                .setPkg(TEST_PACKAGE_NAME)
                .setOpPkg(TEST_PACKAGE_NAME)
                .setUid(TEST_UID)
                .setNotification(new Notification())
                .setUser(UserHandle.CURRENT)
                .build();
        mEntry.setRow(mRow);

        mRemoteInputManager.setUpWithPresenterForTest(mCallback,
                mDelegate, mController);
        for (NotificationLifetimeExtender extender : mRemoteInputManager.getLifetimeExtenders()) {
            extender.setCallback(
                    mock(NotificationLifetimeExtender.NotificationSafeToRemoveCallback.class));
        }
    }

    @Test
    public void testPerformOnRemoveNotification() {
        when(mController.isRemoteInputActive(mEntry)).thenReturn(true);
        mRemoteInputManager.onPerformRemoveNotification(mEntry, mEntry.getKey());

        assertFalse(mEntry.mRemoteEditImeVisible);
        verify(mController).removeRemoteInput(mEntry, null);
    }

    @Test
    public void testShouldExtendLifetime_remoteInputActive() {
        when(mController.isRemoteInputActive(mEntry)).thenReturn(true);

        assertTrue(mRemoteInputActiveExtender.shouldExtendLifetime(mEntry));
    }

    @Test
    public void testShouldExtendLifetime_isSpinning() {
        NotificationRemoteInputManager.FORCE_REMOTE_INPUT_HISTORY = true;
        when(mController.isSpinning(mEntry.getKey())).thenReturn(true);

        assertTrue(mRemoteInputHistoryExtender.shouldExtendLifetime(mEntry));
    }

    @Test
    public void testShouldExtendLifetime_recentRemoteInput() {
        NotificationRemoteInputManager.FORCE_REMOTE_INPUT_HISTORY = true;
        mEntry.lastRemoteInputSent = SystemClock.elapsedRealtime();

        assertTrue(mRemoteInputHistoryExtender.shouldExtendLifetime(mEntry));
    }

    @Test
    public void testShouldExtendLifetime_smartReplySending() {
        NotificationRemoteInputManager.FORCE_REMOTE_INPUT_HISTORY = true;
        when(mSmartReplyController.isSendingSmartReply(mEntry.getKey())).thenReturn(true);

        assertTrue(mSmartReplyHistoryExtender.shouldExtendLifetime(mEntry));
    }

    @Test
    public void testNotificationWithRemoteInputActiveIsRemovedOnCollapse() {
        mRemoteInputActiveExtender.setShouldManageLifetime(mEntry, true /* shouldManage */);

        assertEquals(mLegacyRemoteInputLifetimeExtender.getEntriesKeptForRemoteInputActive(),
                Sets.newArraySet(mEntry));

        mRemoteInputManager.onPanelCollapsed();

        assertTrue(
                mLegacyRemoteInputLifetimeExtender.getEntriesKeptForRemoteInputActive().isEmpty());
    }

    @Test
    public void testRebuildWithRemoteInput_noExistingInput_image() {
        Uri uri = mock(Uri.class);
        String mimeType = "image/jpeg";
        String text = "image inserted";
        StatusBarNotification newSbn =
                mRemoteInputManager.rebuildNotificationWithRemoteInputInserted(
                        mEntry, text, false, mimeType, uri);
        RemoteInputHistoryItem[] messages = (RemoteInputHistoryItem[]) newSbn.getNotification()
                .extras.getParcelableArray(Notification.EXTRA_REMOTE_INPUT_HISTORY_ITEMS);
        assertEquals(1, messages.length);
        assertEquals(text, messages[0].getText());
        assertEquals(mimeType, messages[0].getMimeType());
        assertEquals(uri, messages[0].getUri());
    }

    @Test
    public void testRebuildWithRemoteInput_noExistingInputNoSpinner() {
        StatusBarNotification newSbn =
                mRemoteInputManager.rebuildNotificationWithRemoteInputInserted(
                        mEntry, "A Reply", false, null, null);
        RemoteInputHistoryItem[] messages = (RemoteInputHistoryItem[]) newSbn.getNotification()
                .extras.getParcelableArray(Notification.EXTRA_REMOTE_INPUT_HISTORY_ITEMS);
        assertEquals(1, messages.length);
        assertEquals("A Reply", messages[0].getText());
        assertFalse(newSbn.getNotification().extras
                .getBoolean(Notification.EXTRA_SHOW_REMOTE_INPUT_SPINNER, false));
        assertTrue(newSbn.getNotification().extras
                .getBoolean(Notification.EXTRA_HIDE_SMART_REPLIES, false));
    }

    @Test
    public void testRebuildWithRemoteInput_noExistingInputWithSpinner() {
        StatusBarNotification newSbn =
                mRemoteInputManager.rebuildNotificationWithRemoteInputInserted(
                        mEntry, "A Reply", true, null, null);
        RemoteInputHistoryItem[] messages = (RemoteInputHistoryItem[]) newSbn.getNotification()
                .extras.getParcelableArray(Notification.EXTRA_REMOTE_INPUT_HISTORY_ITEMS);
        assertEquals(1, messages.length);
        assertEquals("A Reply", messages[0].getText());
        assertTrue(newSbn.getNotification().extras
                .getBoolean(Notification.EXTRA_SHOW_REMOTE_INPUT_SPINNER, false));
        assertTrue(newSbn.getNotification().extras
                .getBoolean(Notification.EXTRA_HIDE_SMART_REPLIES, false));
    }

    @Test
    public void testRebuildWithRemoteInput_withExistingInput() {
        // Setup a notification entry with 1 remote input.
        StatusBarNotification newSbn =
                mRemoteInputManager.rebuildNotificationWithRemoteInputInserted(
                        mEntry, "A Reply", false, null, null);
        NotificationEntry entry = new NotificationEntryBuilder()
                .setSbn(newSbn)
                .build();

        // Try rebuilding to add another reply.
        newSbn = mRemoteInputManager.rebuildNotificationWithRemoteInputInserted(
                entry, "Reply 2", true, null, null);
        RemoteInputHistoryItem[] messages = (RemoteInputHistoryItem[]) newSbn.getNotification()
                .extras.getParcelableArray(Notification.EXTRA_REMOTE_INPUT_HISTORY_ITEMS);
        assertEquals(2, messages.length);
        assertEquals("Reply 2", messages[0].getText());
        assertEquals("A Reply", messages[1].getText());
    }

    @Test
    public void testRebuildWithRemoteInput_withExistingInput_image() {
        // Setup a notification entry with 1 remote input.
        Uri uri = mock(Uri.class);
        String mimeType = "image/jpeg";
        String text = "image inserted";
        StatusBarNotification newSbn =
                mRemoteInputManager.rebuildNotificationWithRemoteInputInserted(
                        mEntry, text, false, mimeType, uri);
        NotificationEntry entry = new NotificationEntryBuilder()
                .setSbn(newSbn)
                .build();

        // Try rebuilding to add another reply.
        newSbn = mRemoteInputManager.rebuildNotificationWithRemoteInputInserted(
                entry, "Reply 2", true, null, null);
        RemoteInputHistoryItem[] messages = (RemoteInputHistoryItem[]) newSbn.getNotification()
                .extras.getParcelableArray(Notification.EXTRA_REMOTE_INPUT_HISTORY_ITEMS);
        assertEquals(2, messages.length);
        assertEquals("Reply 2", messages[0].getText());
        assertEquals(text, messages[1].getText());
        assertEquals(mimeType, messages[1].getMimeType());
        assertEquals(uri, messages[1].getUri());
    }

    @Test
    public void testRebuildNotificationForCanceledSmartReplies() {
        // Try rebuilding to remove spinner and hide buttons.
        StatusBarNotification newSbn =
                mRemoteInputManager.rebuildNotificationForCanceledSmartReplies(mEntry);
        assertFalse(newSbn.getNotification().extras
                .getBoolean(Notification.EXTRA_SHOW_REMOTE_INPUT_SPINNER, false));
        assertTrue(newSbn.getNotification().extras
                .getBoolean(Notification.EXTRA_HIDE_SMART_REPLIES, false));
    }


    private class TestableNotificationRemoteInputManager extends NotificationRemoteInputManager {

        TestableNotificationRemoteInputManager(
                Context context,
                FeatureFlags featureFlags,
                NotificationLockscreenUserManager lockscreenUserManager,
                SmartReplyController smartReplyController,
                NotificationEntryManager notificationEntryManager,
                Lazy<Optional<StatusBar>> statusBarOptionalLazy,
                StatusBarStateController statusBarStateController,
                Handler mainHandler,
                RemoteInputUriController remoteInputUriController,
                NotificationClickNotifier clickNotifier,
                ActionClickLogger actionClickLogger,
                DumpManager dumpManager) {
            super(
                    context,
                    featureFlags,
                    lockscreenUserManager,
                    smartReplyController,
                    notificationEntryManager,
                    statusBarOptionalLazy,
                    statusBarStateController,
                    mainHandler,
                    remoteInputUriController,
                    clickNotifier,
                    actionClickLogger,
                    dumpManager);
        }

        public void setUpWithPresenterForTest(Callback callback,
                RemoteInputController.Delegate delegate,
                RemoteInputController controller) {
            super.setUpWithCallback(callback, delegate);
            mRemoteInputController = controller;
        }

        @NonNull
        @Override
        protected LegacyRemoteInputLifetimeExtender createLegacyRemoteInputLifetimeExtender(
                Handler mainHandler,
                NotificationEntryManager notificationEntryManager,
                SmartReplyController smartReplyController) {
            mLegacyRemoteInputLifetimeExtender = new FakeLegacyRemoteInputLifetimeExtender();
            return mLegacyRemoteInputLifetimeExtender;
        }

        class FakeLegacyRemoteInputLifetimeExtender extends LegacyRemoteInputLifetimeExtender {

            @Override
            protected void addLifetimeExtenders() {
                mRemoteInputActiveExtender = new RemoteInputActiveExtender();
                mRemoteInputHistoryExtender = new RemoteInputHistoryExtender();
                mSmartReplyHistoryExtender = new SmartReplyHistoryExtender();
                mLifetimeExtenders.add(mRemoteInputHistoryExtender);
                mLifetimeExtenders.add(mSmartReplyHistoryExtender);
                mLifetimeExtenders.add(mRemoteInputActiveExtender);
            }
        }

    }
}
