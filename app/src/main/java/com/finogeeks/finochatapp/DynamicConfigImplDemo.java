package com.finogeeks.finochatapp;

//import com.tencent.mrs.plugin.IDynamicConfig;
//
//public class DynamicConfigImplDemo implements IDynamicConfig {
//
//    public DynamicConfigImplDemo() {
//    }
//
//    public boolean isFPSEnable() {
//        return true;
//    }
//
//    public boolean isTraceEnable() {
//        return true;
//    }
//
//    public boolean isMatrixEnable() {
//        return true;
//    }
//
//    @Override
//    public String get(String key, String defStr) {
//        return defStr;
//    }
//
//    @Override
//    public int get(String key, int defInt) {
//        if (MatrixEnum.clicfg_matrix_resource_max_detect_times.name().equals(key)) {
//            return 2;
//        }
//
//        if (MatrixEnum.clicfg_matrix_trace_fps_report_threshold.name().equals(key)) {
//            return 10000;
//        }
//
//        if (MatrixEnum.clicfg_matrix_trace_fps_time_slice.name().equals(key)) {
//            return 12000;
//        }
//
//        return defInt;
//    }
//
//    @Override
//    public long get(String key, long defLong) {
//        if (MatrixEnum.clicfg_matrix_trace_fps_report_threshold.name().equals(key)) {
//            return 10000L;
//        }
//
//        if (MatrixEnum.clicfg_matrix_resource_detect_interval_millis.name().equals(key)) {
//            return 2000;
//        }
//
//        return defLong;
//    }
//
//    @Override
//    public boolean get(String key, boolean defBool) {
//        return defBool;
//    }
//
//    @Override
//    public float get(String key, float defFloat) {
//        return defFloat;
//    }
//}