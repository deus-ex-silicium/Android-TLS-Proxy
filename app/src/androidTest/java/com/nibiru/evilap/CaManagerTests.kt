package com.nibiru.evilap

import android.support.test.runner.AndroidJUnit4
import com.nibiru.evilap.pki.CaManager
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class CaManagerTests {

    @Test
    fun generateCaTest() {
        //val ca = CaManager()
        Assert.assertTrue(true)
    }
    @Test
    fun readCaTest(){
        val certStr =
"""-----BEGIN CERTIFICATE-----
MIIEpTCCAo2gAwIBAgIBATANBgkqhkiG9w0BAQ0FADAWMRQwEgYDVQQDDAtGVU5TRUMgSW5jLjAe
Fw0xODA1MTIwOTMzMjdaFw0xODA2MTIwOTMzMjdaMBYxFDASBgNVBAMMC0ZVTlNFQyBJbmMuMIIC
IjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEA1SVc9Owr7e83VF/dc5lKelJM6sUW9D0uVN/d
2jzv++wVXpTY4It4vXiw59P9Gz/bI+ju9urFc+SNU+KyLi55sJMMofKokF+lhCT30UDZZc0SCbmX
hD+fRU0NPiamuPZQV8/k7bR/ap2alBmBFXLoHNWaWLfkzakZDTzk/f5dY103THm88Sh/NpWWjHt+
1wUDONqUX6dNXLt1U/pwMhNEnjOnVHK9SoB3H9ivF7x20FAklN5HhUo3IoUJlddWO1wWTdzgnbkk
U6o/Z7/l/JUHxdSbi7KLSgrHzAJE7tBLbgLQ7OTxYGMJExfMH/QJs0ZnmPfPLVEXiwxx+NfAz4qq
bzFjTJYvwowKeCxRBI3Mkk7wKQpe5YVUVGJ1F+l05EyZw+jewB+HeaUqxmaO7CVVclW1x+UAmIlD
FB0KZGypUH3x4FOj/kGvUxJkWy9FRhPOFIt+yTrOXw/j4WIvQfFHMwdzs7NhLCbLc0L2N/da/qLy
8wQq43MQ2yvCDBWf204qkk1yooppIbGbt1JUzOLOodyH2DBzYoEPkeReLmazuQXiN9kRgc3inTcx
d0uxZh4x9ho1YqrVps3U8DS1SuYEWcyg72mv3zuZP2q3jkcguHlzNrvy74pS9qLMaqxsVpQCHzSu
bik9NUnOp8xdgQNdXI0/abE+yI6H6k0dLDSSup8CAwEAATANBgkqhkiG9w0BAQ0FAAOCAgEASvh9
loi4kA/QeMJ7Ywcvx45PpS0BM5fde2uXqU4uwFyKRF/GB+jBi1Yj43ZvYNS6JThh8jPiuB0NN5b0
ncEHZZqzi98zEMmY71zRiYV2Vtr8U0cdVkWp7zmNWMZjojODDzh7PB2pfCF+zi2HUad3zpuiYgaG
nHWEIC7SX3ryFBdbrs48hRX03G4+gLzPLTFV+ne4lNvwnPSL8teQoxDsumJTyVkHqodeeKTm4sir
JaIbGqae0XK/nsAslfeEO+PhViVSkEGDjNstvQ1CfVZVCuhXgOkbxlj+z27QzXjmf1dbAAKv8zNm
NTP3OD1SnjGCq12iOq+gUr6ZEMiTkFC5G2pDlwP6TVfTymAvAzlIU7z9O1DgCbUQD5/BGMQadJ30
ftCkGI7bF98xE3yC0JtJ9FyP4pQ7H5E2e2xUVpWfCGGLN8pleJuHzKI4pl0/2Puz2upGEZu6g8fC
L+I/cQisk3fORB5rhcBcmiOIp5+r9hxTNOlPGnHPLQDuI6KcRsEUyCk8u7F/d1x1IgQy0x9n5jQH
JH1CJJNwMfDWI+5yXPv5VuUgsXkEeS8NSae3IP2/JWidctO4RevsVc8vLgNU/9wbX9Z/L0Ev9zTQ
DF4I0P2T5O66rYpgBLUFNvIG2Y6GD7vQYY3nxMXNWY45KxwpqwBgRJR5DIQhVnEbXxbPxNs=
-----END CERTIFICATE-----"""
        val ca = CaManager(certStr)
    }

}