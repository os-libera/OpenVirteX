./ovxctl.py -n createNetwork tcp 10.1.10.48 10000 192.168.0.0 16
./ovxctl.py -n createSwitch 1 1
./ovxctl.py -n createSwitch 1 3,4,5
./ovxctl.py -n createSwitch 1 6,7
./ovxctl.py -n createSwitch 1 9
./ovxctl.py -n createSwitch 1 10
./ovxctl.py -n createSwitch 1 11
./ovxctl.py -n createSwitch 1 12
./ovxctl.py -n connectHost 1 1 1 00:00:00:00:00:01
./ovxctl.py -n connectHost 1 1 2 00:00:00:00:00:02
./ovxctl.py -n connectHost 1 3 1 00:00:00:00:00:03
./ovxctl.py -n connectHost 1 5 1 00:00:00:00:00:04
./ovxctl.py -n connectHost 1 7 1 00:00:00:00:00:05
./ovxctl.py -n connectHost 1 7 2 00:00:00:00:00:06
./ovxctl.py -n connectHost 1 10 1 00:00:00:00:00:07
./ovxctl.py -n connectHost 1 11 1 00:00:00:00:00:08
./ovxctl.py -n connectHost 1 12 1 00:00:00:00:00:09
./ovxctl.py -n createLink 1 1/3-2/1,2/2-3/2
./ovxctl.py -n createLink 1 5/3-11/2
./ovxctl.py -n createLink 1 5/4-6/1
./ovxctl.py -n createLink 1 7/4-12/2
./ovxctl.py -n createLink 1 7/5-8/1,8/2-9/1
./ovxctl.py -n createLink 1 9/2-10/2
./ovxctl.py -n createSwitchRoute 1 2 1 2 3/3-4/1,4/2-5/2
./ovxctl.py -n createSwitchRoute 1 2 1 4 3/3-4/1,4/2-5/2
./ovxctl.py -n createSwitchRoute 1 2 1 5 3/3-4/1,4/2-5/2
./ovxctl.py -n createSwitchRoute 1 2 2 3 5/2-4/2,4/1-3/3
./ovxctl.py -n createSwitchRoute 1 2 3 4 3/3-4/1,4/2-5/2
./ovxctl.py -n createSwitchRoute 1 2 3 5 3/3-4/1,4/2-5/2
./ovxctl.py -n createSwitchRoute 1 3 1 3 7/3-6/2
./ovxctl.py -n createSwitchRoute 1 3 2 3 7/3-6/2
./ovxctl.py -n createSwitchRoute 1 3 3 4 6/2-7/3
./ovxctl.py -n createSwitchRoute 1 3 3 5 6/2-7/3
./ovxctl.py -n startNetwork 1

