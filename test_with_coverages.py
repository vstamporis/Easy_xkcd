import os
import subprocess
import shutil
import datetime

apps = "apps"
config1 = "configs/fuzzer1.properties"
config2 = "configs/fuzzer2.properties"
config3 = "configs/fuzzer3.properties"

jacocoReport = "gradlew.bat jacocoTestReport"

command1_monkey = "gradlew.bat clean startMonkey --events=\"1500\" --throttle=\"200\" --epochs=\"30\" --config=\"" + config1 + "\""
command2_monkey = "gradlew.bat clean startMonkey --events=\"2500\" --throttle=\"200\" --epochs=\"3\" --config=\"" + config2 + "\""
command3_monkey = "gradlew.bat clean startMonkey --events=\"2500\" --throttle=\"200\" --epochs=\"3\" --config=\"" + config3 + "\""

command1 = "gradlew.bat clean startBaristaFuzzer --events=\"1500\" --throttle=\"200\" --epochs=\"30\" --config=\"" + config1 + "\""
command2 = "gradlew.bat clean startBaristaFuzzer --events=\"2500\" --throttle=\"200\" --epochs=\"3\" --config=\"" + config2 + "\""
command3 = "gradlew.bat clean startBaristaFuzzer --events=\"2500\" --throttle=\"200\" --epochs=\"3\" --config=\"" + config3 + "\""

commands = [command1, command1_monkey, command1, command1_monkey, command1, command1_monkey]

logfile = open("logfile.log", "w")

logfile.write("Walked to apps\n")
logfile.flush()
dirs = os.listdir('.')

if not (os.path.isdir('reports')):
    os.mkdir("reports")

pathToCopy = os.getcwd() + '/reports'
#print(pathToCopy)

t = 1

for y in commands:
    #p = subprocess.Popen(installDebug, shell=True, universal_newlines=True, stdout=logfile)
    #ret_code = p.wait()
    print("Executing command " +  y + " for app \n")
    logfile.write("Executing command " + y + " for app \n")
    logfile.flush()
    p = subprocess.Popen(y, shell=True, universal_newlines=True, stdout=logfile)
    ret_code = p.wait()
    logfile.flush()

    print(os.getcwd())

    p = subprocess.Popen(jacocoReport, shell=True, universal_newlines=True, stdout=logfile)
    ret_code = p.wait()
    logfile.flush()

    for root, dirs, files in os.walk('.', topdown = True):
        for name in list(set(dirs)):

            if name == 'build':
                print(os.getcwd())
                os.chdir(os.path.join(root, name))
                nested = os.listdir()
                if 'reports' in nested:
                    os.chdir('reports')
                    os.chdir('jacoco')
                    os.chdir('jacocoTestReport')
                    fname = ""
                    if t % 2 != 0:
                        fname = "_Barista"
                    else:
                        fname = "_Monkey"
                    t += 1
                    time = datetime.datetime.now().strftime("%Y-%m-%dT%H-%M-%S") + fname
                    os.mkdir(os.path.join(pathToCopy, time))
                    for item in os.listdir(os.getcwd()):
                        s = os.path.join(os.getcwd(), item)
                        d = os.path.join(pathToCopy + "/" + time, item)
                        if os.path.isdir(s):
                            shutil.copytree(s, d, False, None)
                        else:
                            shutil.copy2(s, d)

    os.chdir('../../../../..')
