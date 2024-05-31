set SCRIPTS_DIR="%~dp0"

set CWD=%cd%

cd %SCRIPTS_DIR%\yaml

kubectl delete -f mydeployment.yaml
kubectl delete -f myjob.yaml
kubectl delete -f np.yaml


helm delete manager
helm delete operator
helm delete processor


cd %CWD%