echo "*****************************"
echo "***** VIRTUAL NETWORK 1 *****"
echo "*****************************"
echo ""
echo ""
./ovxctl.py -n createNetwork tcp 192.168.255.1 10000 192.168.0.0 16
./ovxctl.py -n createSwitch 1 00:00:00:00:00:00:00:6b,00:00:00:00:00:00:00:6c,00:00:00:00:00:00:00:73,00:00:00:00:00:00:00:74
./ovxctl.py -n createSwitch 1 00:00:00:00:00:00:00:6d,00:00:00:00:00:00:00:6e,00:00:00:00:00:00:00:75,00:00:00:00:00:00:00:76
./ovxctl.py -n createSwitch 1 00:00:00:00:00:00:00:6f,00:00:00:00:00:00:00:70,00:00:00:00:00:00:00:71,00:00:00:00:00:00:00:72,00:00:00:00:00:00:00:77,00:00:00:00:00:00:00:78,00:00:00:00:00:00:00:79,00:00:00:00:00:00:00:7a
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
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:6c 1
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:6e 2
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:6f 1
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:70 2
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 1 00:00:00:00:00:01
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 2 00:00:00:00:00:02
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 3 00:00:00:00:00:03
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 4 00:00:00:00:00:04
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:02 1 00:00:00:00:00:05
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:02 2 00:00:00:00:00:06
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:02 3 00:00:00:00:00:07
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:02 4 00:00:00:00:00:08
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:03 1 00:00:00:00:00:09
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:03 2 00:00:00:00:00:0a
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:03 3 00:00:00:00:00:0b
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:03 4 00:00:00:00:00:0c
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:03 5 00:00:00:00:00:0d
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:03 6 00:00:00:00:00:0e
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:03 7 00:00:00:00:00:0f
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:03 8 00:00:00:00:00:10
./ovxctl.py -n connectLink 1 00:a4:23:05:00:00:00:01 5 00:a4:23:05:00:00:00:03 9 00:00:00:00:00:00:00:6c/1-00:00:00:00:00:00:00:67/4,00:00:00:00:00:00:00:67/1-00:00:00:00:00:00:00:65/1,00:00:00:00:00:00:00:65/3-00:00:00:00:00:00:00:69/1,00:00:00:00:00:00:00:69/3-00:00:00:00:00:00:00:6f/1 128
./ovxctl.py -n connectLink 1 00:a4:23:05:00:00:00:02 5 00:a4:23:05:00:00:00:03 10 00:00:00:00:00:00:00:6e/2-00:00:00:00:00:00:00:68/6,00:00:00:00:00:00:00:68/2-00:00:00:00:00:00:00:66/2,00:00:00:00:00:00:00:66/4-00:00:00:00:00:00:00:6a/2,00:00:00:00:00:00:00:6a/4-00:00:00:00:00:00:00:70/2 128
./ovxctl.py -n startNetwork 1

read a
echo "*****************************"
echo "**** DYNAMIC CONFIG VN 1 ****"
echo "*****************************"
echo ""
echo ""
echo ">>>>> Stop the network <<<<<"
echo ""
./ovxctl.py -n stopNetwork 1
read a
echo ""
echo "(Ping should NOT work: h2 ping h10)"
echo ""
read a
echo ">>>>> Start the network <<<<<"
echo ""
./ovxctl.py -n startNetwork 1
read a
echo ""
echo "(Now ping should work again: h2 ping h10)"
echo "(Before proceed, STOP ping!!!)"
echo ""
read a

echo ""
echo ">>>>> Stop the virtual switch 1 <<<<<"
echo ""
./ovxctl.py -n stopSwitch 1 00:a4:23:05:00:00:00:01
read a
echo ""
echo "(Ping should work only partially: h6 ping h10 YES, h2 ping h10 NO)"
echo ""
read a
echo ">>>>> Start the virtual switch again <<<<<"
echo ""
./ovxctl.py -n startSwitch 1 00:a4:23:05:00:00:00:01
read a
echo ""
echo "(Now ping should work again: h2 ping h10)"
echo "(Before proceed, STOP ping!!!)"
echo ""
read a
echo ""
echo ">>>>> Stop the virtual port 1 in virtual switch 1 <<<<<"
echo ""
./ovxctl.py -n stopPort 1 00:a4:23:05:00:00:00:01 1
read a
echo ""
echo "(Ping should work only partially: h2 ping h10 YES, h1 ping h10 NO)"
echo ""
read a
echo ">>>>> Start the virtual port again <<<<<"
echo ""
./ovxctl.py -n startPort 1 00:a4:23:05:00:00:00:01 1
read a
echo ""
echo "(Now ping should work again: h2 ping h10)"
echo "(Before proceed, STOP ping!!!)"
echo ""
read a
echo ">>>>> Remove Virtual Link 1 <<<<<"
echo ""
./ovxctl.py -n disconnectLink 1 1
read a
echo ""
echo "(Ping should work only partially: h6 ping h10 YES, h2 ping h10 NO)"
echo ""
read a
echo ">>>>> Re-add the previously removed Virtual Link <<<<<"
echo ""
./ovxctl.py -n connectLink 1 00:a4:23:05:00:00:00:01 5 00:a4:23:05:00:00:00:03 9 00:00:00:00:00:00:00:6c/1-00:00:00:00:00:00:00:67/4,00:00:00:00:00:00:00:67/1-00:00:00:00:00:00:00:65/1,00:00:00:00:00:00:00:65/3-00:00:00:00:00:00:00:69/1,00:00:00:00:00:00:00:69/3-00:00:00:00:00:00:00:6f/1 128
read a
echo ""
echo "(Now ping should work again: h2 ping h10)"
echo "(Before proceed, STOP ping!!!)"
echo ""
read a
echo ">>>>> Remove Virtual Switch 1 <<<<<"
echo ""
./ovxctl.py -n removeSwitch 1 00:a4:23:05:00:00:00:01
read a
echo ""
echo "(Ping should work only partially: h6 ping h10 YES, h2 ping h10 NO)"
echo ""
read a
echo ">>>>> Re-add the previously removed Virtual Switch <<<<<"
echo ""
./ovxctl.py -n createSwitch 1 00:00:00:00:00:00:00:6b,00:00:00:00:00:00:00:6c,00:00:00:00:00:00:00:73,00:00:00:00:00:00:00:74
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:73 1
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:73 2
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:74 1
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:74 2
./ovxctl.py -n createPort 1 00:00:00:00:00:00:00:6c 1
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 1 00:00:00:00:00:01
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 2 00:00:00:00:00:02
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 3 00:00:00:00:00:03
./ovxctl.py -n connectHost 1 00:a4:23:05:00:00:00:01 4 00:00:00:00:00:04
./ovxctl.py -n connectLink 1 00:a4:23:05:00:00:00:01 5 00:a4:23:05:00:00:00:03 9 00:00:00:00:00:00:00:6c/1-00:00:00:00:00:00:00:67/4,00:00:00:00:00:00:00:67/1-00:00:00:00:00:00:00:65/1,00:00:00:00:00:00:00:65/3-00:00:00:00:00:00:00:69/1,00:00:00:00:00:00:00:69/3-00:00:00:00:00:00:00:6f/1 128
read a
echo ""
echo "(Now ping should work again: h17 ping h22)"
echo ""
