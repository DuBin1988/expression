git add -A
git commit -m "[build] %1"
git tag -a V%1 -m "Release version %1"
git push origin --tags
git push
