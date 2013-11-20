echo "*****************************"
echo "***** VIRTUAL NETWORK 1 *****"
echo "*****************************"
echo ""
echo ""
./ovxctl.py -n createNetwork tcp 192.168.56.5 10000 192.168.0.0 16
./ovxctl.py -n createSwitch 1 00:00:00:00:00:00:00:65,00:00:00:00:00:00:00:66,00:00:00:00:00:00:00:67,00:00:00:00:00:00:00:68,00:00:00:00:00:00:00:69,00:00:00:00:00:00:00:6a,00:00:00:00:00:00:00:6b,00:00:00:00:00:00:00:6c,00:00:00:00:00:00:00:6d,00:00:00:00:00:00:00:6e,00:00:00:00:00:00:00:6f,00:00:00:00:00:00:00:70,00:00:00:00:00:00:00:71,00:00:00:00:00:00:00:72,00:00:00:00:00:00:00:73,00:00:00:00:00:00:00:74,00:00:00:00:00:00:00:75,00:00:00:00:00:00:00:76,00:00:00:00:00:00:00:77,00:00:00:00:00:00:00:78,00:00:00:00:00:00:00:79,00:00:00:00:00:00:00:7a
./ovxctl.py -n createSwitch 1 00:00:00:00:00:00:00:c9,00:00:00:00:00:00:00:ca,00:00:00:00:00:00:00:cb,00:00:00:00:00:00:00:cc,00:00:00:00:00:00:00:cd,00:00:00:00:00:00:00:ce,00:00:00:00:00:00:00:cf,00:00:00:00:00:00:00:d0,00:00:00:00:00:00:00:d1,00:00:00:00:00:00:00:d2,00:00:00:00:00:00:00:d3,00:00:00:00:00:00:00:d4,00:00:00:00:00:00:00:d5,00:00:00:00:00:00:00:d6,00:00:00:00:00:00:00:d7,00:00:00:00:00:00:00:d8,00:00:00:00:00:00:00:d9,00:00:00:00:00:00:00:da,00:00:00:00:00:00:00:db,00:00:00:00:00:00:00:dc,00:00:00:00:00:00:00:dd,00:00:00:00:00:00:00:de
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:73 1
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:73 2
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:74 1
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:74 2
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:75 1
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:75 2
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:76 1
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:76 2
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:77 1
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:77 2
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:78 1
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:78 2
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:79 1
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:79 2
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:7a 1
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:7a 2
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:d7 1
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:d7 2
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:d8 1
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:d8 2
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:d9 1
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:d9 2
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:da 1
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:da 2
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:db 1
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:db 2
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:dc 1
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:dc 2
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:dd 1
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:dd 2
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:de 1
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:de 2
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:65 5
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:c9 5
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 1 00:00:00:00:00:01
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 2 00:00:00:00:00:02
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 3 00:00:00:00:00:03
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 4 00:00:00:00:00:04
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 5 00:00:00:00:00:05
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 6 00:00:00:00:00:06
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 7 00:00:00:00:00:07
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 8 00:00:00:00:00:08
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 9 00:00:00:00:00:09
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 10 00:00:00:00:00:0a
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 11 00:00:00:00:00:0b
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 12 00:00:00:00:00:0c
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 13 00:00:00:00:00:0d
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 14 00:00:00:00:00:0e
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 15 00:00:00:00:00:0f
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 16 00:00:00:00:00:10
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:02 1 00:00:00:00:00:11
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:02 2 00:00:00:00:00:12
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:02 3 00:00:00:00:00:13
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:02 4 00:00:00:00:00:14
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:02 5 00:00:00:00:00:15
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:02 6 00:00:00:00:00:16
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:02 7 00:00:00:00:00:17
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:02 8 00:00:00:00:00:18
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:02 9 00:00:00:00:00:19
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:02 10 00:00:00:00:00:1a
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:02 11 00:00:00:00:00:1b
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:02 12 00:00:00:00:00:1c
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:02 13 00:00:00:00:00:1d
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:02 14 00:00:00:00:00:1e
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:02 15 00:00:00:00:00:1f
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:02 16 00:00:00:00:00:20
./ovxctl.py -n connectLink 1 00:a4:23:05:00:00:00:01 17 00:a4:23:05:00:00:00:02 17 spf 1
./ovxctl.py -n startNetwork 1

echo "*****************************"
echo "***** VIRTUAL NETWORK 2 *****"
echo "*****************************"
echo ""
echo ""
./ovxctl.py -n createNetwork tcp 192.168.56.5 20000 192.168.0.0 16
./ovxctl.py -n createSwitch 2 00:00:00:00:00:00:00:67,00:00:00:00:00:00:00:68,00:00:00:00:00:00:00:6b,00:00:00:00:00:00:00:6c,00:00:00:00:00:00:00:6d,00:00:00:00:00:00:00:6e,00:00:00:00:00:00:00:73,00:00:00:00:00:00:00:74,00:00:00:00:00:00:00:75,00:00:00:00:00:00:00:76
./ovxctl.py -n createSwitch 2 00:00:00:00:00:00:00:69,00:00:00:00:00:00:00:6a,00:00:00:00:00:00:00:6f,00:00:00:00:00:00:00:70,00:00:00:00:00:00:00:71,00:00:00:00:00:00:00:72,00:00:00:00:00:00:00:77,00:00:00:00:00:00:00:78,00:00:00:00:00:00:00:79,00:00:00:00:00:00:00:7a
./ovxctl.py -n createSwitch 2 00:00:00:00:00:00:00:cb,00:00:00:00:00:00:00:cc,00:00:00:00:00:00:00:cf,00:00:00:00:00:00:00:d0,00:00:00:00:00:00:00:d1,00:00:00:00:00:00:00:d2,00:00:00:00:00:00:00:d7,00:00:00:00:00:00:00:d8,00:00:00:00:00:00:00:d9,00:00:00:00:00:00:00:da
./ovxctl.py -n createSwitch 2 00:00:00:00:00:00:00:cd,00:00:00:00:00:00:00:ce,00:00:00:00:00:00:00:d3,00:00:00:00:00:00:00:d4,00:00:00:00:00:00:00:d5,00:00:00:00:00:00:00:d6,00:00:00:00:00:00:00:db,00:00:00:00:00:00:00:dc,00:00:00:00:00:00:00:dd,00:00:00:00:00:00:00:de
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:73 3
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:73 4
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:74 3
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:74 4
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:75 3
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:75 4
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:76 3
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:76 4
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:77 3
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:77 4
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:78 3
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:78 4
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:79 3
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:79 4
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:7a 3
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:7a 4
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:d7 3
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:d7 4
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:d8 3
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:d8 4
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:d9 3
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:d9 4
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:da 3
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:da 4
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:db 3
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:db 4
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:dc 3
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:dc 4
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:dd 3
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:dd 4
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:de 3
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:de 4
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:67 1
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:68 1
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:69 1
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:6a 1
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:cb 1
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:cc 1
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:cd 1
./ovxctl.py -n createPort 2 00:00:00:00:00:00:00:ce 1
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:01 1 00:00:00:00:00:21
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:01 2 00:00:00:00:00:22
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:01 3 00:00:00:00:00:23
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:01 4 00:00:00:00:00:24
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:01 5 00:00:00:00:00:25
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:01 6 00:00:00:00:00:26
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:01 7 00:00:00:00:00:27
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:01 8 00:00:00:00:00:28
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:02 1 00:00:00:00:00:29
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:02 2 00:00:00:00:00:2a
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:02 3 00:00:00:00:00:2b
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:02 4 00:00:00:00:00:2c
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:02 5 00:00:00:00:00:2d
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:02 6 00:00:00:00:00:2e
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:02 7 00:00:00:00:00:2f
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:02 8 00:00:00:00:00:30
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:03 1 00:00:00:00:00:31
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:03 2 00:00:00:00:00:32
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:03 3 00:00:00:00:00:33
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:03 4 00:00:00:00:00:34
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:03 5 00:00:00:00:00:35
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:03 6 00:00:00:00:00:36
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:03 7 00:00:00:00:00:37
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:03 8 00:00:00:00:00:38
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:04 1 00:00:00:00:00:39
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:04 2 00:00:00:00:00:3a
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:04 3 00:00:00:00:00:3b
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:04 4 00:00:00:00:00:3c
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:04 5 00:00:00:00:00:3d
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:04 6 00:00:00:00:00:3e
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:04 7 00:00:00:00:00:3f
./ovxctl.py -n connectHost 2 00:a4:23:05:00:00:00:04 8 00:00:00:00:00:40
./ovxctl.py -n connectLink 2 00:a4:23:05:00:00:00:01 10 00:a4:23:05:00:00:00:02 9 spf 1
./ovxctl.py -n connectLink 2 00:a4:23:05:00:00:00:02 10 00:a4:23:05:00:00:00:03 9 spf 1
./ovxctl.py -n connectLink 2 00:a4:23:05:00:00:00:03 10 00:a4:23:05:00:00:00:04 9 spf 1
./ovxctl.py -n startNetwork 2


echo "*****************************"
echo "***** VIRTUAL NETWORK 3 *****"
echo "*****************************"
echo ""
echo ""
./ovxctl.py -n createNetwork tcp 192.168.56.5 30000 192.168.0.0 16
./ovxctl.py -n createSwitch 3 00:00:00:00:00:00:00:6b,00:00:00:00:00:00:00:6c,00:00:00:00:00:00:00:73,00:00:00:00:00:00:00:74
./ovxctl.py -n createSwitch 3 00:00:00:00:00:00:00:6d,00:00:00:00:00:00:00:6e,00:00:00:00:00:00:00:75,00:00:00:00:00:00:00:76
./ovxctl.py -n createSwitch 3 00:00:00:00:00:00:00:6f,00:00:00:00:00:00:00:70,00:00:00:00:00:00:00:77,00:00:00:00:00:00:00:78
./ovxctl.py -n createSwitch 3 00:00:00:00:00:00:00:71,00:00:00:00:00:00:00:72,00:00:00:00:00:00:00:79,00:00:00:00:00:00:00:7a
./ovxctl.py -n createSwitch 3 00:00:00:00:00:00:00:cf,00:00:00:00:00:00:00:d0,00:00:00:00:00:00:00:d7,00:00:00:00:00:00:00:d8
./ovxctl.py -n createSwitch 3 00:00:00:00:00:00:00:d1,00:00:00:00:00:00:00:d2,00:00:00:00:00:00:00:d9,00:00:00:00:00:00:00:da
./ovxctl.py -n createSwitch 3 00:00:00:00:00:00:00:d3,00:00:00:00:00:00:00:d4,00:00:00:00:00:00:00:db,00:00:00:00:00:00:00:dc
./ovxctl.py -n createSwitch 3 00:00:00:00:00:00:00:d5,00:00:00:00:00:00:00:d6,00:00:00:00:00:00:00:dd,00:00:00:00:00:00:00:de
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:73 5
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:73 6
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:74 5
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:74 6
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:75 5
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:75 6
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:76 5
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:76 6
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:77 5
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:77 6
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:78 5
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:78 6
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:79 5
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:79 6
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:7a 5
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:7a 6
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:d7 5
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:d7 6
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:d8 5
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:d8 6
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:d9 5
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:d9 6
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:da 5
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:da 6
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:db 5
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:db 6
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:dc 5
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:dc 6
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:dd 5
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:dd 6
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:de 5
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:de 6
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:6b 1
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:6c 1
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:6d 1
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:6e 1
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:6f 1
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:70 1
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:71 1
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:72 1
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:cf 1
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:d0 1
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:d1 1
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:d2 1
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:d3 1
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:d4 1
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:d5 1
./ovxctl.py -n createPort 3 00:00:00:00:00:00:00:d6 1
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:01 1 00:00:00:00:00:41
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:01 2 00:00:00:00:00:42
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:01 3 00:00:00:00:00:43
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:01 4 00:00:00:00:00:44
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:02 1 00:00:00:00:00:45
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:02 2 00:00:00:00:00:46
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:02 3 00:00:00:00:00:47
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:02 4 00:00:00:00:00:48
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:03 1 00:00:00:00:00:49
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:03 2 00:00:00:00:00:4a
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:03 3 00:00:00:00:00:4b
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:03 4 00:00:00:00:00:4c
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:04 1 00:00:00:00:00:4d
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:04 2 00:00:00:00:00:4e
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:04 3 00:00:00:00:00:4f
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:04 4 00:00:00:00:00:50
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:05 1 00:00:00:00:00:51
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:05 2 00:00:00:00:00:52
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:05 3 00:00:00:00:00:53
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:05 4 00:00:00:00:00:54
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:06 1 00:00:00:00:00:55
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:06 2 00:00:00:00:00:56
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:06 3 00:00:00:00:00:57
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:06 4 00:00:00:00:00:58
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:07 1 00:00:00:00:00:59
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:07 2 00:00:00:00:00:5a
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:07 3 00:00:00:00:00:5b
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:07 4 00:00:00:00:00:5c
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:08 1 00:00:00:00:00:5d
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:08 2 00:00:00:00:00:5e
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:08 3 00:00:00:00:00:5f
./ovxctl.py -n connectHost 3 00:a4:23:05:00:00:00:08 4 00:00:00:00:00:60
./ovxctl.py -n connectLink 3 00:a4:23:05:00:00:00:01 6 00:a4:23:05:00:00:00:02 5 spf 1
./ovxctl.py -n connectLink 3 00:a4:23:05:00:00:00:02 6 00:a4:23:05:00:00:00:03 5 spf 1
./ovxctl.py -n connectLink 3 00:a4:23:05:00:00:00:03 6 00:a4:23:05:00:00:00:04 5 spf 1
./ovxctl.py -n connectLink 3 00:a4:23:05:00:00:00:04 6 00:a4:23:05:00:00:00:05 5 spf 1
./ovxctl.py -n connectLink 3 00:a4:23:05:00:00:00:05 6 00:a4:23:05:00:00:00:06 5 spf 1
./ovxctl.py -n connectLink 3 00:a4:23:05:00:00:00:06 6 00:a4:23:05:00:00:00:07 5 spf 1
./ovxctl.py -n connectLink 3 00:a4:23:05:00:00:00:07 6 00:a4:23:05:00:00:00:08 5 spf 1
./ovxctl.py -n startNetwork 3

