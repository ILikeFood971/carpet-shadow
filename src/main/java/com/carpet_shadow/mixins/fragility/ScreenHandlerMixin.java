package com.carpet_shadow.mixins.fragility;

import com.carpet_shadow.CarpetShadowSettings;
import com.carpet_shadow.Globals;
import com.carpet_shadow.interfaces.ShadowItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin {

    String shadowId1 = null;

    @Shadow
    public static boolean canInsertItemIntoSlot(@Nullable Slot slot, ItemStack stack, boolean allowOverflow) {
        return false;
    }

    @Shadow
    public abstract ItemStack getCursorStack();

    @Redirect(method = "internalOnSlotClick", slice = @Slice(
            from = @At(value = "INVOKE", target = "Lnet/minecraft/screen/slot/Slot;canTakeItems(Lnet/minecraft/entity/player/PlayerEntity;)Z", ordinal = 1)),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/slot/Slot;setStack(Lnet/minecraft/item/ItemStack;)V", ordinal = 0)
    )
    public void remove_shadow_stack(Slot instance, ItemStack stack) {
        shadowId1 = ((ShadowItem) (Object) stack).getShadowId();
        instance.setStack(stack);
    }

    @Redirect(method = "internalOnSlotClick", slice = @Slice(
            from = @At(value = "INVOKE", target = "Lnet/minecraft/screen/slot/Slot;canTakeItems(Lnet/minecraft/entity/player/PlayerEntity;)Z", ordinal = 1)),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/ScreenHandler;setCursorStack(Lnet/minecraft/item/ItemStack;)V", ordinal = 1)
    )
    public void remove_shadow_stack(ScreenHandler instance, ItemStack stack) {
        String shadowId2 = ((ShadowItem) (Object) stack).getShadowId();
        if (CarpetShadowSettings.shadowItemFragilityFixes && shadowId1 != null && shadowId1.equals(shadowId2)) {
            instance.setCursorStack(ItemStack.EMPTY);
        } else {
            instance.setCursorStack(stack);
        }
        shadowId1 = null;
    }

    @Redirect(method = "internalOnSlotClick", slice = @Slice(
            from = @At(value = "INVOKE", target = "Lnet/minecraft/screen/slot/Slot;canTakeItems(Lnet/minecraft/entity/player/PlayerEntity;)Z")
    ),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/ScreenHandler;transferSlot(Lnet/minecraft/entity/player/PlayerEntity;I)Lnet/minecraft/item/ItemStack;"))
    public ItemStack fix_shift(ScreenHandler instance, PlayerEntity player, int index) {
        if (CarpetShadowSettings.shadowItemFragilityFixes) {
            Slot og = instance.slots.get(index);
            ItemStack og_item = og.getStack();
            if (((ShadowItem) (Object) og_item).getShadowId() != null) {
                ItemStack mirror = og_item.copy();
                ((ShadowItem) (Object) mirror).setShadowId(((ShadowItem) (Object) og_item).getShadowId());
                og.setStack(mirror);
                ItemStack ret = instance.transferSlot(player, index);
                if (ret == ItemStack.EMPTY) {
                    og_item = Globals.getByIdOrAdd(((ShadowItem) (Object) og_item).getShadowId(), og_item);
                    og.setStack(og_item);
                    og_item.setCount(mirror.getCount());
                }
                return ret;
            }
        }
        return instance.transferSlot(player, index);
    }

    @Redirect(method = "insertItem", slice = @Slice(
            from = @At(value = "INVOKE", target = "Lnet/minecraft/screen/slot/Slot;getStack()Lnet/minecraft/item/ItemStack;", ordinal = 1)
    ),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;split(I)Lnet/minecraft/item/ItemStack;", ordinal = 1))
    public ItemStack fix_shift(ItemStack instance, int amount) {
        if (CarpetShadowSettings.shadowItemFragilityFixes && ((ShadowItem) (Object) instance).getShadowId() != null) {
            String shadow_id = ((ShadowItem) (Object) instance).getShadowId();
            ItemStack og_item = Globals.getByIdOrNull(shadow_id);
            if (og_item != null) {
                og_item.setCount(instance.getCount());
                instance.setCount(0);
                return og_item;
            }
        }
        return instance.split(amount);
    }

    @Redirect(method = "insertItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isStackable()Z", ordinal = 0))
    public boolean fix_shift2(ItemStack instance) {
        if (CarpetShadowSettings.shadowItemFragilityFixes && ((ShadowItem) (Object) instance).getShadowId() != null) {
            return false;
        }
        return instance.isStackable();
    }

    @Redirect(method = "internalOnSlotClick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/ScreenHandler;canInsertItemIntoSlot(Lnet/minecraft/screen/slot/Slot;Lnet/minecraft/item/ItemStack;Z)Z"))
    public boolean fixQuickCraft(Slot slot, ItemStack stack, boolean allowOverflow) {
        if (CarpetShadowSettings.shadowItemFragilityFixes &&
                (((ShadowItem) (Object) slot.getStack()).getShadowId() != null || ((ShadowItem) (Object) this.getCursorStack()).getShadowId() != null)) {
            return false;
        }
        return canInsertItemIntoSlot(slot, stack, allowOverflow);
    }

}
