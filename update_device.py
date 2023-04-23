import random
import hashlib
import json


# https://github.com/dylan-fan/imeiDemo
# https://www.cnblogs.com/bohr/p/7093392.html

def get_IMEI():
    r1 = 1000000 + random.randint(1,9000000)
    r2 = 1000000 + random.randint(1,9000000)
    input = str(r1) + str(r2)
    ch = list(input)
    a = 0
    b = 0
    for i in range(len(ch)):
        tt = int(ch[i])
        if (i % 2 == 0):
            a = a + tt
        else:
            temp = tt * 2
            b = b + temp / 10 + temp % 10

    last = int((a + b) % 10)
    if (last == 0):
        last = 0
    else:
        last = 10 - last
    new_imei = input + str(last)
    print("imei: " + new_imei)
    # 64136 37286 26742
    return new_imei

def get_IMSI_md5():
    title = "4600"
    second = 0
    while second == 0:
        second = random.randint(1,8)
    r1 = 10000 + random.randint(1,90000)
    r2 = 10000 + random.randint(1,90000)
    new_imsi = title + "" + str(second) + "" + str(r1) + "" + str(r2)
    new_imsi_md5 = hashlib.md5(new_imsi.encode("utf-8")).hexdigest()
    print("imsi: " +new_imsi)
    print("imsi_md5: " + new_imsi_md5)
    return new_imsi_md5


def randomMac():
    maclist = []
    for i in range(1,7):
        randstr = "".join(random.sample("0123456789ABCDEF",2))
        maclist.append(randstr)
    randmac = ":".join(maclist)
    # randmac = "%3A".join(maclist)
    print("mac: " +randmac)
    return randmac

if __name__ == '__main__':
    imei = get_IMEI()
    imsi_md5 = get_IMSI_md5()
    mac = randomMac()
    with open("./device.json","r") as f:
        json_obj = json.loads(f.read())
        json_obj["data"]["imei"] = imei
        json_obj["data"]["imsiMd5"] = imsi_md5
        json_obj["data"]["macAddress"] = mac
    with open("./device.json", "w") as f:
        f.write(json.dumps(json_obj))
    
    