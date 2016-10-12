﻿namespace CustomActions
{

    public class CommonProperties
    {
        public const string InstallDir = "INSTALLDIR";
        public const string IsJavaInstalled = "IS_JAVA_INSTALLED";
    }

    public class HawkCDServerProperties : CommonProperties
    {
        public const string HostName = "HOST_NAME";
        public const string IsDeafultRedisPortInUse = "IS_DEFAULT_REDIS_PORT_IN_USE";
        public const string DeafultRedisPort = "DEFAULT_REDIS_PORT";
        public const string NewRedisPort = "NEW_REDIS_PORT";
    }

    public class HawkCDAgentProperties : CommonProperties
    {
        public const string ServerAddress = "SERVER_ADDRESS";
    }
}