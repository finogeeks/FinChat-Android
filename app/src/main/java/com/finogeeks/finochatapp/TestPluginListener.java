package com.finogeeks.finochatapp;

//import android.content.Context;
//
//import com.tencent.matrix.plugin.DefaultPluginListener;
//import com.tencent.matrix.report.Issue;
//import com.tencent.matrix.util.MatrixLog;
//
//import java.lang.ref.SoftReference;
//
//public class TestPluginListener extends DefaultPluginListener {
//
//    private static final String TAG = "TestPluginListener";
//
//    private SoftReference<Context> softReference;
//
//    public TestPluginListener(Context context) {
//        super(context);
//        softReference = new SoftReference<>(context);
//    }
//
//    @Override
//    public void onReportIssue(Issue issue) {
//        super.onReportIssue(issue);
//        MatrixLog.e(TAG, issue.toString());
//    }
//}