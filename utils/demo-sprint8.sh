echo "*****************************"
echo "***** VIRTUAL NETWORK 1 *****"
echo "*****************************"
echo ""
echo ""
./ovxctl.py -n createNetwork tcp 10.4.1.13 10000 192.168.0.0 16
./ovxctl.py -n createSwitch 1 19,1,20,2,22,12,21,14,18,13,4,9,6,17,16,3,5,10,7,8,15,11
./ovxctl.py -n connectHost 1 15 1 00:00:00:00:00:01
./ovxctl.py -n connectHost 1 16 1 00:00:00:00:00:02
./ovxctl.py -n connectHost 1 17 1 00:00:00:00:00:03
./ovxctl.py -n connectHost 1 18 1 00:00:00:00:00:04
./ovxctl.py -n connectHost 1 19 1 00:00:00:00:00:05
./ovxctl.py -n connectHost 1 20 1 00:00:00:00:00:06
./ovxctl.py -n connectHost 1 21 1 00:00:00:00:00:07
./ovxctl.py -n connectHost 1 22 1 00:00:00:00:00:08
./ovxctl.py -n createSwitchRoute 1 46200400562356225 1 2 15/6-8/3,8/4-16/6
./ovxctl.py -n createSwitchRoute 1 46200400562356225 1 3 15/6-8/3,8/1-3/4,3/5-9/1,9/3-17/5
./ovxctl.py -n createSwitchRoute 1 46200400562356225 1 4 15/6-8/3,8/1-3/4,3/5-9/1,9/4-18/5
./ovxctl.py -n createSwitchRoute 1 46200400562356225 1 5 15/6-8/3,8/1-3/4,3/1-1/1,1/3-5/1,5/4-12/1,12/3-19/6
./ovxctl.py -n createSwitchRoute 1 46200400562356225 1 6 15/6-8/3,8/1-3/4,3/1-1/1,1/3-5/1,5/4-12/1,12/4-20/6
./ovxctl.py -n createSwitchRoute 1 46200400562356225 1 7 15/6-8/3,8/1-3/4,3/1-1/1,1/3-5/1,5/6-14/1,14/3-21/6
./ovxctl.py -n createSwitchRoute 1 46200400562356225 1 8 15/6-8/3,8/1-3/4,3/1-1/1,1/3-5/1,5/6-14/1,14/4-22/6
./ovxctl.py -n createSwitchRoute 1 46200400562356225 2 3 16/6-8/4,8/1-3/4,3/5-9/1,9/3-17/5
./ovxctl.py -n createSwitchRoute 1 46200400562356225 2 4 16/6-8/4,8/1-3/4,3/5-9/1,9/4-18/5
./ovxctl.py -n createSwitchRoute 1 46200400562356225 2 5 16/6-8/4,8/1-3/4,3/1-1/1,1/3-5/1,5/4-12/1,12/3-19/6
./ovxctl.py -n createSwitchRoute 1 46200400562356225 2 6 16/6-8/4,8/1-3/4,3/1-1/1,1/3-5/1,5/4-12/1,12/4-20/6
./ovxctl.py -n createSwitchRoute 1 46200400562356225 2 7 16/6-8/4,8/1-3/4,3/1-1/1,1/3-5/1,5/6-14/1,14/3-21/6
./ovxctl.py -n createSwitchRoute 1 46200400562356225 2 8 16/6-8/4,8/1-3/4,3/1-1/1,1/3-5/1,5/6-14/1,14/4-22/6
./ovxctl.py -n createSwitchRoute 1 46200400562356225 3 4 17/5-9/3,9/4-18/5
./ovxctl.py -n createSwitchRoute 1 46200400562356225 3 5 17/5-9/3,9/1-3/5,3/1-1/1,1/3-5/1,5/4-12/1,12/3-19/6
./ovxctl.py -n createSwitchRoute 1 46200400562356225 3 6 17/5-9/3,9/1-3/5,3/1-1/1,1/3-5/1,5/4-12/1,12/4-20/6
./ovxctl.py -n createSwitchRoute 1 46200400562356225 3 7 17/5-9/3,9/1-3/5,3/1-1/1,1/3-5/1,5/6-14/1,14/3-21/6
./ovxctl.py -n createSwitchRoute 1 46200400562356225 3 8 17/5-9/3,9/1-3/5,3/1-1/1,1/3-5/1,5/6-14/1,14/4-22/6
./ovxctl.py -n createSwitchRoute 1 46200400562356225 4 5 18/5-9/4,9/1-3/5,3/1-1/1,1/3-5/1,5/4-12/1,12/3-19/6
./ovxctl.py -n createSwitchRoute 1 46200400562356225 4 6 18/5-9/4,9/1-3/5,3/1-1/1,1/3-5/1,5/4-12/1,12/4-20/6
./ovxctl.py -n createSwitchRoute 1 46200400562356225 4 7 18/5-9/4,9/1-3/5,3/1-1/1,1/3-5/1,5/6-14/1,14/3-21/6
./ovxctl.py -n createSwitchRoute 1 46200400562356225 4 8 18/5-9/4,9/1-3/5,3/1-1/1,1/3-5/1,5/6-14/1,14/4-22/6
./ovxctl.py -n createSwitchRoute 1 46200400562356225 5 6 19/6-12/3,12/4-20/6
./ovxctl.py -n createSwitchRoute 1 46200400562356225 5 7 19/6-12/3,12/1-5/4,5/6-14/1,14/3-21/6
./ovxctl.py -n createSwitchRoute 1 46200400562356225 5 8 19/6-12/3,12/1-5/4,5/6-14/1,14/4-22/6
./ovxctl.py -n createSwitchRoute 1 46200400562356225 6 7 20/6-12/4,12/1-5/4,5/6-14/1,14/3-21/6
./ovxctl.py -n createSwitchRoute 1 46200400562356225 6 8 20/6-12/4,12/1-5/4,5/6-14/1,14/4-22/6
./ovxctl.py -n createSwitchRoute 1 46200400562356225 7 8 21/6-14/3,14/4-22/6
./ovxctl.py -n startNetwork 1

read a
echo "*****************************"
echo "***** VIRTUAL NETWORK 2 *****"
echo "*****************************"
echo ""
echo ""
./ovxctl.py -n createNetwork tcp 10.4.1.13 20000 192.168.0.0 16
./ovxctl.py -n createSwitch 2 1,3,4,7,8,9,10,15,16,17,18
./ovxctl.py -n createSwitch 2 2,5,6,11,12,13,14,19,20,21,22
./ovxctl.py -n connectHost 2 15 2 00:00:00:00:00:09
./ovxctl.py -n connectHost 2 16 2 00:00:00:00:00:0a
./ovxctl.py -n connectHost 2 17 2 00:00:00:00:00:0b
./ovxctl.py -n connectHost 2 18 2 00:00:00:00:00:0c
./ovxctl.py -n connectHost 2 19 2 00:00:00:00:00:0d
./ovxctl.py -n connectHost 2 20 2 00:00:00:00:00:0e
./ovxctl.py -n connectHost 2 21 2 00:00:00:00:00:0f
./ovxctl.py -n connectHost 2 22 2 00:00:00:00:00:10
./ovxctl.py -n createLink 2 1/3-5/1
./ovxctl.py -n createSwitchRoute 2 46200400562356225 1 2 15/6-8/3,8/4-16/6
./ovxctl.py -n createSwitchRoute 2 46200400562356225 1 3 15/5-7/3,7/2-4/3,4/5-9/2,9/3-17/5
./ovxctl.py -n createSwitchRoute 2 46200400562356225 1 4 15/5-7/3,7/2-4/3,4/5-9/2,9/4-18/5
./ovxctl.py -n createSwitchRoute 2 46200400562356225 1 5 15/6-8/3,8/2-4/4,4/1-1/2
./ovxctl.py -n createSwitchRoute 2 46200400562356225 2 3 16/6-8/4,8/2-4/4,4/5-9/2,9/3-17/5
./ovxctl.py -n createSwitchRoute 2 46200400562356225 2 4 16/6-8/4,8/2-4/4,4/5-9/2,9/4-18/5
./ovxctl.py -n createSwitchRoute 2 46200400562356225 2 5 16/6-8/4,8/2-4/4,4/1-1/2
./ovxctl.py -n createSwitchRoute 2 46200400562356225 3 4 17/5-9/3,9/4-18/5
./ovxctl.py -n createSwitchRoute 2 46200400562356225 3 5 17/5-9/3,9/1-3/5,3/1-1/1
./ovxctl.py -n createSwitchRoute 2 46200400562356225 4 5 18/5-9/4,9/1-3/5,3/1-1/1
./ovxctl.py -n createSwitchRoute 2 46200400562356226 1 2 19/6-12/3,12/4-20/6
./ovxctl.py -n createSwitchRoute 2 46200400562356226 1 3 19/5-11/3,11/2-6/3,6/5-13/2,13/3-21/5
./ovxctl.py -n createSwitchRoute 2 46200400562356226 1 4 19/5-11/3,11/2-6/3,6/5-13/2,13/4-22/5
./ovxctl.py -n createSwitchRoute 2 46200400562356226 1 5 19/6-12/3,12/1-5/4
./ovxctl.py -n createSwitchRoute 2 46200400562356226 2 3 20/6-12/4,12/2-6/4,6/5-13/2,13/3-21/5
./ovxctl.py -n createSwitchRoute 2 46200400562356226 2 4 20/6-12/4,12/2-6/4,6/5-13/2,13/4-22/5
./ovxctl.py -n createSwitchRoute 2 46200400562356226 2 5 20/6-12/4,12/1-5/4
./ovxctl.py -n createSwitchRoute 2 46200400562356226 3 4 21/5-13/3,13/4-22/5
./ovxctl.py -n createSwitchRoute 2 46200400562356226 3 5 21/5-13/3,13/1-5/5
./ovxctl.py -n createSwitchRoute 2 46200400562356226 4 5 22/5-13/4,13/1-5/5
./ovxctl.py -n startNetwork 2

read a
echo "*****************************"
echo "***** VIRTUAL NETWORK 3 *****"
echo "*****************************"
echo ""
echo ""
./ovxctl.py -n createNetwork tcp 10.4.1.13 30000 192.168.0.0 16
./ovxctl.py -n createSwitch 3 3
./ovxctl.py -n createSwitch 3 4
./ovxctl.py -n createSwitch 3 5
./ovxctl.py -n createSwitch 3 6
./ovxctl.py -n createSwitch 3 15
./ovxctl.py -n createSwitch 3 16
./ovxctl.py -n createSwitch 3 17
./ovxctl.py -n createSwitch 3 18
./ovxctl.py -n createSwitch 3 19
./ovxctl.py -n createSwitch 3 20
./ovxctl.py -n createSwitch 3 21
./ovxctl.py -n createSwitch 3 22
./ovxctl.py -n connectHost 3 15 3 00:00:00:00:00:11
./ovxctl.py -n connectHost 3 16 3 00:00:00:00:00:12
./ovxctl.py -n connectHost 3 17 3 00:00:00:00:00:13
./ovxctl.py -n connectHost 3 18 3 00:00:00:00:00:14
./ovxctl.py -n connectHost 3 19 3 00:00:00:00:00:15
./ovxctl.py -n connectHost 3 20 3 00:00:00:00:00:16
./ovxctl.py -n connectHost 3 21 3 00:00:00:00:00:17
./ovxctl.py -n connectHost 3 22 3 00:00:00:00:00:18
./ovxctl.py -n createLink 3 3/1-1/1,1/2-4/1
./ovxctl.py -n createLink 3 5/2-2/3,2/4-6/2
./ovxctl.py -n createLink 3 4/1-1/2,1/3-5/1
./ovxctl.py -n createLink 3 15/5-7/3,7/1-3/3
./ovxctl.py -n createLink 3 16/6-8/4,8/1-3/4
./ovxctl.py -n createLink 3 17/5-9/3,9/2-4/5
./ovxctl.py -n createLink 3 18/6-10/4,10/2-4/6
./ovxctl.py -n createLink 3 19/5-11/3,11/1-5/3
./ovxctl.py -n createLink 3 20/6-12/4,12/1-5/4
./ovxctl.py -n createLink 3 21/5-13/3,13/2-6/5
./ovxctl.py -n createLink 3 22/6-14/4,14/2-6/6
./ovxctl.py -n startNetwork 3

read a
echo "*****************************"
echo "***** VIRTUAL NETWORK 4 *****"
echo "*****************************"
echo ""
echo ""
./ovxctl.py -n createNetwork tcp 10.4.1.13 40000 192.168.0.0 16
./ovxctl.py -n createSwitch 4 1
./ovxctl.py -n createSwitch 4 2
./ovxctl.py -n createSwitch 4 7,8,15,16
./ovxctl.py -n createSwitch 4 9,10,17,18
./ovxctl.py -n createSwitch 4 11,19
./ovxctl.py -n createSwitch 4 12,20
./ovxctl.py -n createSwitch 4 13,21
./ovxctl.py -n createSwitch 4 14,22
./ovxctl.py -n connectHost 4 15 4 00:00:00:00:00:19
./ovxctl.py -n connectHost 4 16 4 00:00:00:00:00:1a
./ovxctl.py -n connectHost 4 17 4 00:00:00:00:00:1b
./ovxctl.py -n connectHost 4 18 4 00:00:00:00:00:1c
./ovxctl.py -n connectHost 4 19 4 00:00:00:00:00:1d
./ovxctl.py -n connectHost 4 20 4 00:00:00:00:00:1e
./ovxctl.py -n connectHost 4 21 4 00:00:00:00:00:1f
./ovxctl.py -n connectHost 4 22 4 00:00:00:00:00:20
./ovxctl.py -n createLink 4 1/1-3/1,3/3-7/1
./ovxctl.py -n createLink 4 1/2-4/1,4/6-10/2
./ovxctl.py -n createLink 4 8/2-4/4,4/2-2/2
./ovxctl.py -n createLink 4 1/3-5/1,5/3-11/1
./ovxctl.py -n createLink 4 1/4-6/1,6/4-12/2
./ovxctl.py -n createLink 4 2/3-5/2,5/5-13/1
./ovxctl.py -n createLink 4 2/4-6/2,6/6-14/2
./ovxctl.py -n createSwitchRoute 4 46200400562356227 1 2 15/5-7/3,7/4-16/5
./ovxctl.py -n createSwitchRoute 4 46200400562356227 1 3 15/5-7/3
./ovxctl.py -n createSwitchRoute 4 46200400562356227 1 4 15/6-8/3
./ovxctl.py -n createSwitchRoute 4 46200400562356227 2 3 16/5-7/4
./ovxctl.py -n createSwitchRoute 4 46200400562356227 2 4 16/6-8/4
./ovxctl.py -n createSwitchRoute 4 46200400562356227 3 4 7/4-16/5,16/6-8/4
./ovxctl.py -n createSwitchRoute 4 46200400562356228 1 2 17/6-10/3,10/4-18/6
./ovxctl.py -n createSwitchRoute 4 46200400562356228 1 3 17/6-10/3
./ovxctl.py -n createSwitchRoute 4 46200400562356228 2 3 18/6-10/4
./ovxctl.py -n createSwitchRoute 4 46200400562356229 1 2 19/5-11/3
./ovxctl.py -n createSwitchRoute 4 46200400562356230 1 2 20/6-12/4
./ovxctl.py -n createSwitchRoute 4 46200400562356231 1 2 21/5-13/3
./ovxctl.py -n createSwitchRoute 4 46200400562356232 1 2 22/6-14/4
./ovxctl.py -n startNetwork 4

read a
echo "*****************************"
echo "**** DYNAMIC CONFIG VN 3 ****"
echo "*****************************"
echo ""
echo ""
echo ">>>>> Stop the network <<<<<"
echo ""
./ovxctl.py -n stopNetwork 3
read a
echo ""
echo "(Ping should NOT work: h17 ping h22)"
echo ""
read a
echo ">>>>> Start the network <<<<<"
echo ""
./ovxctl.py -n startNetwork 3
read a
echo ""
echo "(Now ping should work again: h17 ping h22)"
echo "(Before proceed, STOP ping!!!)"
echo ""
read a
echo ">>>>> Remove Virtual Link 3 <<<<<"
echo ""
./ovxctl.py -n removeLink 3 3
read a
echo ""
echo "(Ping should work only partially: h17 ping h19 YES, h17 ping h22 NO)"
echo ""
read a
echo ">>>>> Re-add the previously removed Virtual Link <<<<<"
echo ""
./ovxctl.py -n createLink 3 4/1-1/2,1/3-5/1
read a
echo ""
echo "(Now ping should work again: h17 ping h22)"
echo "(Before proceed, STOP ping!!!)"
echo ""
read a
echo ">>>>> Remove Virtual Switch 1 <<<<<"
echo ""
./ovxctl.py -n removeSwitch 3 1
read a
echo ""
echo "(Ping should work only partially: h19 ping h22 YES, h17 ping h22 NO)"
echo ""
read a
echo ">>>>> Re-add the previously removed Virtual Switch <<<<<"
echo ""
./ovxctl.py -n createSwitch 3 3
./ovxctl.py -n createLink 3 3/1-1/1,1/2-4/1
./ovxctl.py -n createLink 3 15/5-7/3,7/1-3/3
./ovxctl.py -n createLink 3 16/6-8/4,8/1-3/4
read a
echo ""
echo "(Now ping should work again: h17 ping h22)"
echo ""