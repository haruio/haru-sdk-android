package com.haru.callback;

/**
 * 서버로 데이터를 업로드할 시, 이 콜백을 사용하면 진행 상황을 퍼센트 (%)로 받을 수 있다.
 * {@link com.haru.HaruFile#saveInBackground(SaveCallback)}
 */
public interface SaveWithProgressCallback extends SaveCallback {
    /**
     * 데이터 업로드 중 중간중간 진행 상황을 업데이트할 수 있게 호출된다.
     * @param percentage 퍼센트 (Double)
     */
    public void progress(double percentage);
}
