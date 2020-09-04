# Additional ProGuard rules to be used to determine which classes are compiled into the main dex file.
# If set, rules from this file are used in combination with the default rules used by the build system.

# web3j
-keep class org.web3j.crypto.WalletFile{*;}
-keep class org.web3j.tx.Contract{*;}