package com.azazel.framework;

public class AzConstants {

    public static final long DEFAULT_TIMEOUT = 1000 * 60;

    public interface TASK_STATUS_CODE {

        public static final int PROCESS_TIMEOUT = 1;
        public static final int PREPARE_FAILED = 2;
    }

    public interface ServiceType {
        public static final int EXTRACT_KEY = 1000000;

        public static final int NONE = 100;
        public static final int BACKUP = 101;
        public static final int RESTORE = 102;
        public static final int GET_DETAILS = 103;
    }

    public interface Status {
        public static final int EXTRACT_KEY = 1000;

        public static final int OPERATION_START = 201;
        public static final int OPERATION_PROGRESS = 202;
        public static final int OPERATION_PAUSED = 203;
        public static final int OPERATION_RESUMED = 204;
        public static final int OPERATION_FINISHED = 205;

    }

    public interface ResultCode {
        public static final int EXTRACT_KEY = 1;

        public static final int SUCCESS = 300;
        public static final int FAIL = 301;

        public static final int DO_NOTHING = 302;

        public static final int FAIL_HTTP = 303;
        public static final int FAIL_JSON = 304;
        public static final int FAIL_AUTHENTICATION = 305;
        public static final int FAIL_USER_CANCELED = 306;
        public static final int FAIL_FILE_WRITE_ERR = 307;
        public static final int FAIL_THREAD = 308;
        public static final int FAIL_INITIALIZE = 309;
        public static final int FAIL_RESTORE_READY = 310;
        public static final int FAIL_SERVER_STORAGE_FULL = 311;
        public static final int FAIL_FILE_IO = 312;
        public static final int FAIL_OEM_INSERT = 313;
        public static final int FAIL_OEM_GET = 314;
        public static final int FAIL_SERVER_ERR = 315;
        public static final int FAIL_IN_PROCESS = 316;
        public static final int FAIL_SOCKET = 317;
        public static final int FAIL_NO_PERMISSION = 318;
        public static final int FAIL_HTTP_TIMEOUT = 319;
        public static final int FAIL_AND_RETRY = 320;

    }


    public static int makeMassageCode(int serviceType, int status, int rCode) {
        return (serviceType * ServiceType.EXTRACT_KEY)
                + (status * Status.EXTRACT_KEY)
                + (rCode * ResultCode.EXTRACT_KEY);
    }

    public static int getServiceType(int msgCode) {
        return msgCode / ServiceType.EXTRACT_KEY;
    }

    public static int getStatus(int msgCode) {
        return (msgCode % ServiceType.EXTRACT_KEY) / Status.EXTRACT_KEY;
    }

    public static int getRCode(int msgCode) {
        return (msgCode % Status.EXTRACT_KEY);
    }

}
