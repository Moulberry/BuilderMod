package codes.moulberry.buildermod.customtool;

import codes.moulberry.buildermod.LaserPointer;

public class LaserPointerTool implements CustomTool {

    @Override
    public void leftClick() {
        CustomTool.raycastBlock((blockHitResult -> {
            LaserPointer.getInstance().addPoint(blockHitResult.getPos());
        }));
    }

    @Override
    public void rightClick() {
        LaserPointer.getInstance().startChain();
    }

}
