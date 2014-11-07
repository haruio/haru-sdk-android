package com.haru.social;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.haru.R;
import com.haru.User;
import com.kakao.APIErrorResult;
import com.kakao.AuthType;
import com.kakao.MeResponseCallback;
import com.kakao.Session;

import com.haru.callback.LoginCallback;
import com.kakao.UserManagement;
import com.kakao.UserProfile;
import com.kakao.helper.StoryProtocol;
import com.kakao.helper.TalkProtocol;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Kakao를 통해 Haru 회원가입 / 로그인을 할 수 있게 해주는 도구이다.
 */
public class KakaoLoginUtils {

    private static boolean isLogining = false;
    private static WeakReference<Activity> currentActivity;
    private static LoginCallback currentCallback;

    /**
     *  Kakao SDK로 로그인 이후, 로그인 함수를 통해 받은 Access Token을 가지고 로그인한다.
     *  callback을 통해 User 객체가 반환되고, 에러 발생시 HaruException을 반환한다.
     *
     *  @param callback Login Callback
     */
    public static void logInAfterKakaoLogined(final LoginCallback callback) {
        final Session currentSession = Session.getCurrentSession();
        if (currentSession == null) {
            throw new IllegalAccessError("Kakao SDK를 통한 로그인 (Session.initializeSession) 이후 호출하세요.");

        } else if (currentSession.isOpened()) {
            throw new IllegalAccessError("세션이 아직 열리지 않았습니다. "
                    + "Kakao SDK를 통한 로그인 (Session.initializeSession) 이후 호출하세요.");
        }

        // Get current user
        UserManagement.requestMe(new MeResponseCallback() {
            @Override
            protected void onSuccess(final UserProfile userProfile) {
                // Log in into haru server
                User.socialLogin("kakao",
                        String.valueOf(userProfile.getId()),
                        currentSession.getAccessToken(),
                        callback);
            }

            @Override
            protected void onNotSignedUp() {
                Log.e("Haru", "Failed to request kakao user info : not signed up");
            }
            @Override
            protected void onSessionClosedFailure(final APIErrorResult errorResult) {
                Log.e("Haru", "Failed to request kakao user info : Session expired : "
                        + errorResult.getErrorMessage());
            }
            @Override
            protected void onFailure(final APIErrorResult errorResult) {
                Log.e("Haru", "Failed to request kakao user info : " + errorResult.getErrorMessage());
            }
        });
    }

    /**
     *  Facebook SDK의 함수를 직접 호출해줄 필요 없이, 자동으로 로그인해준다.
     *  callback을 통해 User 객체가 반환되고, 에러 발생시 HaruException을 반환한다.
     *
     *  @param activity Facebook Login을 호출하는 activity
     *  @param callback Login Callback
     */
    public static void logIn(Activity activity, final LoginCallback callback) {
        if (isLogining) {
            Log.i("Haru", "로그인중에는 로그인 함수를 중복 호출하실 수 없습니다.");
            return;
        }

        isLogining = true;
        currentActivity = new WeakReference<Activity>(activity);
        currentCallback = callback;

        // 카톡 또는 카스가 존재하면 옵션 다이얼로그를 보여주고, 존재하지 않으면 바로 직접 로그인창.
        final List<AuthType> authTypes = getAuthTypes();
        if(authTypes.size() == 1){
            Session.getCurrentSession().open(authTypes.get(0));
        } else {
            showKakaoServiceChooser(authTypes);
        }
    }


    /**
     * 카카오톡, 카카오스토리 등의 설치 유무를 체크하여 가능한 로그인 수단의 목록을 반환한다.
     * @return 카카오 로그인 수단 리스트 (AuthType)
     */
    private static List<AuthType> getAuthTypes() {
        final List<AuthType> availableAuthTypes = new ArrayList<AuthType>();
        Activity activity = currentActivity.get();
        
        if (TalkProtocol.existCapriLoginActivityInTalk(activity)) {
            availableAuthTypes.add(AuthType.KAKAO_TALK);
        }
        if (StoryProtocol.existCapriLoginActivityInStory(activity)){
            availableAuthTypes.add(AuthType.KAKAO_STORY);
        }
        availableAuthTypes.add(AuthType.KAKAO_ACCOUNT);

        final AuthType[] selectedAuthTypes = Session.getCurrentSession().getAuthTypes();
        availableAuthTypes.retainAll(Arrays.asList(selectedAuthTypes));

        // 개발자가 설정한 것과 available 한 타입이 없다면 직접계정 입력이 뜨도록 한다.
        if(availableAuthTypes.size() == 0){
            availableAuthTypes.add(AuthType.KAKAO_ACCOUNT);
        }
        return availableAuthTypes;
    }

    /**
     * 카카오 서비스들중 하나를 선택해 로그인할 수 있게 한다.
     * @param authTypes getAuthTypes()에서 얻은, 사용자 폰에 설치된 가능한 로그인 수단들
     */
    private static void showKakaoServiceChooser(final List<AuthType> authTypes){
        final List<Item> itemList = new ArrayList<Item>();
        final Activity activity = currentActivity.get();

        if (authTypes.contains(AuthType.KAKAO_TALK)) {
            itemList.add(new Item(R.string.com_kakao_kakaotalk_account,
                    R.drawable.kakaotalk_icon,
                    AuthType.KAKAO_TALK));
        }
        if (authTypes.contains(AuthType.KAKAO_STORY)) {
            itemList.add(new Item(R.string.com_kakao_kakaostory_account,
                    R.drawable.kakaostory_icon,
                    AuthType.KAKAO_STORY));
        }
        if (authTypes.contains(AuthType.KAKAO_ACCOUNT)) {
            itemList.add(new Item(R.string.com_kakao_other_kakaoaccount,
                    R.drawable.kakaoaccount_icon,
                    AuthType.KAKAO_ACCOUNT));
        }
        itemList.add(new Item(R.string.com_kakao_account_cancel, 0, null)); //no icon for this one

        final Item[] items = itemList.toArray(new Item[itemList.size()]);

        final ListAdapter adapter = new ArrayAdapter<Item>(
                activity,
                android.R.layout.select_dialog_item,
                android.R.id.text1, items){

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                TextView tv = (TextView)v.findViewById(android.R.id.text1);

                tv.setText(items[position].textId);
                tv.setTextSize(15);
                tv.setGravity(Gravity.CENTER);
                if(position == itemList.size() -1) {
                    tv.setBackgroundResource(R.drawable.kakao_cancel_button_background);
                } else {
                    tv.setBackgroundResource(R.drawable.kakao_account_button_background);
                }
                tv.setCompoundDrawablesWithIntrinsicBounds(items[position].icon, 0, 0, 0);

                int dp5 = (int) (5 * activity.getResources().getDisplayMetrics().density + 0.5f);
                tv.setCompoundDrawablePadding(dp5);

                return v;
            }
        };

        new AlertDialog.Builder(activity)
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int position) {
                        final AuthType authType = items[position].authType;
                        if(authType == null){
                            dialog.dismiss();
                        } else{
                            Session.getCurrentSession().open(authType);
                        }
                    }
                }).create().show();
    }

    private static class Item {
        public final int textId;
        public final int icon;
        public final AuthType authType;
        public Item(final int textId, final Integer icon, final AuthType authType) {
            this.textId = textId;
            this.icon = icon;
            this.authType = authType;
        }
    }

    /**
     * {@link com.haru.social.KakaoLoginUtils#logIn(android.app.Activity, com.haru.callback.LoginCallback)}
     * Activity를 통해 로그인했을 경우에 해당 액티비티의 onResume에서 호출해야 하는 함수이다.
     */
    public static void onResume() {
        if (isLogining && Session.getCurrentSession().isOpened()){
            isLogining = false;

            // now we need to retrieve user informations
            // and then.. log in into haru server!
            logInAfterKakaoLogined(currentCallback);
        }
    }
}

