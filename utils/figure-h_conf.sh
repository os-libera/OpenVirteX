./ovxctl.py createNetwork tcp 192.168.56.1 10000 192.168.0.0 16
./ovxctl.py createVSwitch 1 1,2,3
./ovxctl.py createVSwitch 1 4
./ovxctl.py createVSwitch 1 6
./ovxctl.py connectHost 1 1 3 00:00:00:00:00:0d
./ovxctl.py connectHost 1 3 3 00:00:00:00:00:0e
./ovxctl.py connectHost 1 4 3 00:00:00:00:00:0f
./ovxctl.py connectHost 1 6 3 00:00:00:00:00:10
./ovxctl.py createVLink 1 2/5-5/3,5/5-4/4
./ovxctl.py createVLink 1 4/4-5/5,5/4-6/4
./ovxctl.py createVSwitchRoute 1 1 1 2 1/4-2/3,2/4-3/4
./ovxctl.py createVSwitchRoute 1 1 1 3 1/4-2/3
./ovxctl.py createVSwitchRoute 1 1 2 3 3/4-2/4
./ovxctl.py bootNetwork 1

./ovxctl.py createNetwork tcp 192.168.56.1 20000 192.168.0.0 16
./ovxctl.py createVSwitch 2 1
./ovxctl.py createVSwitch 2 2
./ovxctl.py createVSwitch 2 5
./ovxctl.py createVSwitch 2 6
./ovxctl.py connectHost 2 1 1 00:00:00:00:00:01
./ovxctl.py connectHost 2 2 1 00:00:00:00:00:02
./ovxctl.py connectHost 2 5 1 00:00:00:00:00:03
./ovxctl.py connectHost 2 6 1 00:00:00:00:00:04
./ovxctl.py createVLink 2 1/4-2/3
./ovxctl.py createVLink 2 1/4-2/3,2/5-5/3
./ovxctl.py createVLink 2 2/5-5/3,5/4-6/4
./ovxctl.py bootNetwork 2

./ovxctl.py createNetwork tcp 192.168.56.1 30000 192.168.0.0 16
./ovxctl.py createVSwitch 3 2,3,5,6
./ovxctl.py createVSwitch 3 1
./ovxctl.py connectHost 3 1 2 00:00:00:00:00:07
./ovxctl.py connectHost 3 3 1 00:00:00:00:00:08
./ovxctl.py connectHost 3 6 2 00:00:00:00:00:09
./ovxctl.py createVLink 3 1/4-2/3
./ovxctl.py createVSwitchRoute 3 1 1 2 3/4-2/4,2/5-5/3,5/4-6/4
./ovxctl.py createVSwitchRoute 3 1 1 3 3/4-2/4
./ovxctl.py createVSwitchRoute 3 1 2 3 6/4-5/4,5/3-2/5
./ovxctl.py bootNetwork 3
