@echo off
echo Building proto files.
for %%i in (*.proto) do (
  echo %%i
  protoc --java_out=.. "%%i"
)
echo Done.