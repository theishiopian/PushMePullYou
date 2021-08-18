package com.theishiopian.PushPull.Blocks;

import com.theishiopian.PushPull.PushMePullYou;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;

public class WinchBlock extends DirectionalBlock
{
    public static BooleanProperty CAN_PULL = BooleanProperty.create("can_pull");

    private static final PistonBaseBlock dummyPiston = new PistonBaseBlock(true, Properties.of(Material.PISTON));

    public WinchBlock(Properties properties)
    {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(CAN_PULL, true));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_55125_) {
        p_55125_.add(FACING, CAN_PULL);
    }

    public BlockState rotate(BlockState state, Rotation rotation)
    {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirror)
    {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite().getOpposite()).setValue(CAN_PULL, true);
    }

    //signal state logic
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos neighbor, boolean flag)
    {
        if (!world.isClientSide)
        {
            //PushMePullYou.LOGGER.info("changed");

            boolean canPull = state.getValue(CAN_PULL);
            boolean hasSignal = GetSignal(world, pos, state.getValue(FACING));

            //if things start triggering twice or stop triggering check here
            if(canPull && hasSignal)
            {
                world.setBlock(pos, state.setValue(CAN_PULL, false), 0);
                world.blockEvent(pos, this, 0, state.getValue(FACING).get3DDataValue());
            }
            else if(!canPull && !hasSignal)
            {
                world.setBlock(pos, state.setValue(CAN_PULL, true), 0);
            }
        }
    }

    //do the funny
    @Override
    public boolean triggerEvent(BlockState triggerState, Level world, BlockPos position, int flagA, int direction)
    {
        Direction dir = Direction.from3DDataValue(direction);
        BlockPos chainPushEnd = GetEndOfChain(world, position, dir);//the block AFTER the last block in the chain
        BlockPos chainPullEnd = GetEndOfChain(world, position, dir.getOpposite());//the block AFTER the last block in the chain
        BlockState chainState = Blocks.CHAIN.defaultBlockState();

        //for pushing, NOT pulling
        BlockState toReplace = world.getBlockState(chainPushEnd);
        boolean toFluid = world.getFluidState(chainPushEnd).getType() == Fluids.WATER;

        boolean hasPushed = false;

        if(dir == Direction.DOWN)
        {
            BlockPos above = position.relative(dir.getOpposite());
            BlockState blockAbove = world.getBlockState(above);
            boolean chainAbove = blockAbove.getBlock().equals(Blocks.CHAIN) && blockAbove.getValue(ChainBlock.AXIS).equals(dir.getAxis());

            if(PullChainFromContainer(world, above) || chainAbove)
            {
                if(toReplace.getMaterial().isReplaceable() || moveBlocks(world, chainPushEnd.relative(dir.getOpposite()), dir, true))
                {
                    chainState = chainState.setValue(ChainBlock.AXIS, dir.getAxis()).setValue(ChainBlock.WATERLOGGED, toFluid);
                    hasPushed = true;
                    world.setBlockAndUpdate(chainPushEnd, chainState);
                }
            }
        }

        BlockPos pullMinus1 = chainPullEnd.relative(dir);

        if(world.getBlockState(pullMinus1).getBlock().equals(Blocks.CHAIN))
        {
            world.setBlockAndUpdate(pullMinus1, Blocks.AIR.defaultBlockState());
            moveBlocks(world, pullMinus1.relative(dir), dir.getOpposite(), false);
            PushMePullYou.LOGGER.info("pulling");

            if(!hasPushed && !PushChainToContainer(world, position.relative(dir)))
            {
                //try to insert chain
                //else spit it out

                //TODO: this is always firing, i believe PushChain is not working
                DropChainAt(world, position.relative(dir));
            }
        }

        return false;
    }

    private boolean GetSignal(Level world, BlockPos pos, Direction dir)
    {
        for(Direction direction : Direction.values()) {
            if (direction != dir && direction != dir.getOpposite() && world.hasSignal(pos.relative(direction), direction))
            {
                return true;
            }
        }
        return false;
    }

    //TODO: add height limit checks
    //TODO: add out of bounds checks
    private BlockPos GetEndOfChain(Level world, BlockPos startingPosition, Direction dir)
    {
        BlockPos next = startingPosition.relative(dir);

        if(world.getBlockState(next).getBlock().equals(Blocks.CHAIN))
        {
            return GetEndOfChain(world, next, dir);
        }

        return next;
    }

    private boolean PullChainFromContainer(Level world, BlockPos position)
    {
        Container container = GetContainerAt(world, position);

        if(container != null)
        {
            for(int slot = 0; slot < container.getContainerSize(); slot ++)
            {
                if(container.getItem(slot).getItem().equals(Items.CHAIN))
                {
                    container.removeItem(slot, 1);
                    container.setChanged();
                    return true;
                }
            }
        }

        return false;
    }

    private boolean PushChainToContainer(Level world, BlockPos position)
    {
        Container container = GetContainerAt(world, position);
        PushMePullYou.LOGGER.info(container);
        if(container != null)
        {
            for(int slot = 0; slot < container.getContainerSize(); slot ++)
            {
                PushMePullYou.LOGGER.info(slot);

                ItemStack chainStack = new ItemStack(Items.CHAIN, 1);
                if(container.canPlaceItem(slot, chainStack))
                {
                    ItemStack stackAt = container.getItem(slot);
                    PushMePullYou.LOGGER.info(stackAt);

                    if(stackAt.getItem().equals(Items.CHAIN) && 1 + stackAt.getCount() <= Items.CHAIN.getMaxStackSize())
                    {
                        //combine stacks
                        stackAt.setCount(stackAt.getCount() + 1);
                        container.setItem(slot, stackAt);
                        return true;
                    }

                    if(stackAt.isEmpty())
                    {
                        container.setItem(slot, chainStack);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private Container GetContainerAt(Level world, BlockPos position)
    {
        BlockState state = world.getBlockState(position);
        Block block = state.getBlock();
        Container container = null;

        if(state.hasBlockEntity())
        {
            BlockEntity blockEntity = world.getBlockEntity(position);
            if(blockEntity instanceof  Container)
            {
                container = (Container)blockEntity;

                if (container instanceof ChestBlockEntity && block instanceof ChestBlock)
                {
                    container = ChestBlock.getContainer((ChestBlock)block, state, world, position, true);
                }
            }
        }

        return container;
    }

    private void DropChainAt(Level world, BlockPos position)
    {
        double x = position.getX() + 0.5;
        double y = position.getY() + 0.5;
        double z = position.getZ() + 0.5;

        ItemEntity droppedItem = new ItemEntity(world, x, y, z, new ItemStack(Items.CHAIN));
        world.addFreshEntity(droppedItem);
    }

    //todo: move to lib
    private boolean moveBlocks(Level world, BlockPos positionToTryToMove, Direction direction, boolean extending)
    {
        return dummyPiston.moveBlocks(world, positionToTryToMove, direction, extending);
    }
}
