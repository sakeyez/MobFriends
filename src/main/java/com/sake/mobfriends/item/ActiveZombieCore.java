package com.sake.mobfriends.item;

import com.sake.mobfriends.client.gui.CombatCoreTooltip;
import com.sake.mobfriends.entity.CombatZombie;
import com.sake.mobfriends.init.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import com.sake.mobfriends.client.gui.CombatCoreTooltip;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ActiveZombieCore extends AbstractCoreItem {
    public ActiveZombieCore(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide() || context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }

        ItemStack stack = context.getItemInHand();
        CompoundTag storedNBT = stack.get(ModDataComponents.STORED_ZOMBIE_NBT.get());
        UUID storedUUID = getZombieUUID(stack); // 获取核心绑定的UUID

        if (storedNBT != null && storedUUID != null && level instanceof ServerLevel serverLevel) {
            // 加载实体
            Entity entity = EntityType.loadEntityRecursive(storedNBT, level, e -> {
                // --- 【核心修正】 ---
                // 在实体生成时，强制将它的UUID设置为我们核心里储存的那个！
                e.setUUID(storedUUID);
                e.setPos(Vec3.atCenterOf(context.getClickedPos().above()));
                return e;
            });

            if (entity != null) {
                serverLevel.addFreshEntity(entity);
                stack.remove(ModDataComponents.STORED_ZOMBIE_NBT.get());
                //音效
                level.playSound(null, context.getClickedPos(), SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 1.0F, 1.0F);
                return InteractionResult.SUCCESS;
            }
        }

        return super.useOn(context);
    }

    public static void storeZombie(ItemStack stack, CombatZombie zombie) {
        if (zombie.isAlive()) {
            // 使用 saveWithoutId 来获取所有数据，这比 .save() 更可控
            CompoundTag nbt = zombie.saveWithoutId(new CompoundTag());

            // 【核心修复】
            // 我们不再依赖 zombie.save() 来添加实体ID，而是手动添加。
            // 这确保了加载时能正确识别实体类型。
            nbt.putString("id", EntityType.getKey(zombie.getType()).toString());

            // 将NBT数据存入核心物品
            stack.set(ModDataComponents.STORED_ZOMBIE_NBT.get(), nbt);
        }
    }

    @Override
    public Component getName(ItemStack pStack) {
        return super.getName(pStack).plainCopy().withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        CompoundTag nbt = stack.get(ModDataComponents.STORED_ZOMBIE_NBT.get());
        if (nbt != null) {
            // 【修改】读取所有需要的数据
            int level = nbt.getInt("WarriorLevel");
            int happiness = nbt.getInt("WarriorHappiness");
            int levelCap = nbt.getInt("WarriorLevelCap");
            // 8 是 ListTag 的 NBT ID
            int diningFoodsCount = nbt.contains("EatenDiningFoods", 9) ? nbt.getList("EatenDiningFoods", 8).size() : 0;
            int ritualFoodsCount = nbt.contains("EatenRitualFoods", 9) ? nbt.getList("EatenRitualFoods", 8).size() : 0;

            return Optional.of(new CombatCoreTooltip(nbt, level, happiness, levelCap, diningFoodsCount, ritualFoodsCount));
        }
        return Optional.empty();
    }

    @Override
    public boolean isFoil(ItemStack pStack) {
        return pStack.has(ModDataComponents.STORED_ZOMBIE_NBT.get());
    }

    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        if (pStack.has(ModDataComponents.STORED_ZOMBIE_NBT.get())) {
            pTooltipComponents.add(Component.translatable("tooltip.mob_friends.active_zombie_core.stored").withStyle(ChatFormatting.BLUE));
        } else {
            pTooltipComponents.add(Component.translatable("tooltip.mob_friends.active_zombie_core.linked").withStyle(ChatFormatting.GREEN));
        }
        super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
    }
}